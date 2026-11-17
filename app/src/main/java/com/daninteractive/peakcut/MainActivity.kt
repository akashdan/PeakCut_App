package com.daninteractive.peakcut

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class CaptionStyle(val label: String) {
    ANTON("Style 1: Anton Bold Pop"),
    ARCHIVO("Style 2: Archivo Box"),
    POPPINS("Style 3: Poppins Clean")
}

data class CreatorWorkspace(
    val id: String,
    var name: String,
    var channelUrl: String,
    var platform: String,
    var defaultStyle: CaptionStyle,
    var autoPost: Boolean,
    var spacingHours: Int,
    var dailyCap: Int,
    val logs: MutableList<String>
)

data class ClipItem(
    val id: String,
    val workspaceId: String,
    var title: String,
    var hook: String,
    val sourceTitle: String,
    val durationSec: Int,
    var style: CaptionStyle,
    val score: Int,
    var transcript: String,
    val localUri: Uri? = null
)

data class PipelineJob(
    val id: String,
    val title: String,
    var stage: String,
    var progress: Float
)

data class LiveSession(
    val id: String,
    val url: String,
    val workspaceId: String,
    var uptimeSec: Int,
    var loudnessDb: Int,
    var chatPerMin: Int,
    var clipsCaptured: Int,
    var transcriptTail: String
)

data class SocialAccount(
    val platform: String,
    var connected: Boolean,
    var handle: String,
    val auditNote: String,
    val redirectUri: String
)

data class QueuedPost(
    val id: String,
    val clipId: String,
    val title: String,
    val platform: String,
    var status: String,
    val window: String
)

data class AiUsageRow(
    val source: String,
    val providerModel: String,
    val requests: Int,
    val tokensIn: Int,
    val tokensOut: Int,
    val clipsFound: Int
)

data class AdminUser(
    val email: String,
    var isAdmin: Boolean,
    val jobsCount: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PeakCutTheme {
                PeakCutApp()
            }
        }
    }
}

@Composable
fun PeakCutTheme(content: @Composable () -> Unit) {
    val darkColors = darkColorScheme(
        primary = Color(0xFFF97316),
        onPrimary = Color(0xFF0C0E12),
        background = Color(0xFF0C0E12),
        surface = Color(0xFF13161D),
        surfaceVariant = Color(0xFF191D26),
        onBackground = Color(0xFFF1F5F9),
        onSurface = Color(0xFFF1F5F9),
        outline = Color(0xFF252A36)
    )
    MaterialTheme(colorScheme = darkColors, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeakCutApp() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf("studio") }
    var accountsEnabled by remember { mutableStateOf(false) }
    var signedInEmail by remember { mutableStateOf("owner@localhost") }
    var showUpdateDialog by remember { mutableStateOf(false) }

    // AI configuration
    var aiProvider by remember { mutableStateOf("gemini") }
    var aiModels by remember { mutableStateOf("gemini-2.5-flash") }
    var aiKey by remember { mutableStateOf("") }
    var aiBaseUrl by remember { mutableStateOf("") }

    val providerModels = remember {
        mapOf(
            "gemini" to listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash"),
            "openai" to listOf("gpt-4.1-mini", "gpt-4o-mini", "gpt-4.1"),
            "xai" to listOf("grok-3-mini", "grok-3"),
            "groq" to listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant"),
            "openrouter" to listOf("google/gemini-2.5-flash", "anthropic/claude-3.5-sonnet"),
            "anthropic" to listOf("claude-3-5-haiku-latest", "claude-3-7-sonnet-latest"),
            "ollama" to listOf("qwen2.5:14b", "llama3.2:latest"),
            "custom" to listOf("custom-openai-model"),
            "none" to emptyList()
        )
    }

    val workspaces = remember {
        mutableStateListOf(
            CreatorWorkspace(
                id = "ws-1",
                name = "Tech & AI Podcast",
                channelUrl = "https://youtube.com/@techpodcast",
                platform = "YouTube",
                defaultStyle = CaptionStyle.ANTON,
                autoPost = true,
                spacingHours = 4,
                dailyCap = 3,
                logs = mutableListOf("Rendered 3 clips from 'Why Local AI Models Won'", "Auto-posted Clip #1 to YouTube Shorts")
            ),
            CreatorWorkspace(
                id = "ws-2",
                name = "Nightly Gaming Stream",
                channelUrl = "https://twitch.tv/nightgamer",
                platform = "Twitch",
                defaultStyle = CaptionStyle.ARCHIVO,
                autoPost = false,
                spacingHours = 2,
                dailyCap = 5,
                logs = mutableListOf("Recorded 42m rolling buffer", "Clipped clutch moment via loudness + chat spike")
            )
        )
    }

    val clips = remember {
        mutableStateListOf(
            ClipItem(
                id = "clip-101",
                workspaceId = "ws-1",
                title = "Why Self-Hosted AI Beats Subscriptions",
                hook = "STOP paying \$30/mo for basic video clips",
                sourceTitle = "Building Open Source Tools in 2026",
                durationSec = 34,
                style = CaptionStyle.ANTON,
                score = 94,
                transcript = "You do not need a cloud subscription just to cut a podcast into vertical shorts. Run the speech model locally and bring your own API key."
            ),
            ClipItem(
                id = "clip-102",
                workspaceId = "ws-2",
                title = "1v4 Clutch at Final Circle",
                hook = "Chat went completely wild on this play",
                sourceTitle = "Twitch VOD - Ranked Marathon",
                durationSec = 26,
                style = CaptionStyle.ARCHIVO,
                score = 91,
                transcript = "Wait watch the smoke on the left side. No way he hits that flick shot with two bullets left!"
            )
        )
    }

    var selectedClipId by remember { mutableStateOf("clip-101") }
    val activeJobs = remember { mutableStateListOf<PipelineJob>() }
    val liveSessions = remember {
        mutableStateListOf(
            LiveSession(
                id = "live-1",
                url = "https://twitch.tv/nightgamer",
                workspaceId = "ws-2",
                uptimeSec = 1842,
                loudnessDb = -14,
                chatPerMin = 48,
                clipsCaptured = 2,
                transcriptTail = "Okay chat, if we win this round I am giving away..."
            )
        )
    }

    val socialAccounts = remember {
        mutableStateListOf(
            SocialAccount("YouTube", true, "@TechPodcastShorts", "Private until Google API audit", "https://your-domain/oauth/youtube/callback"),
            SocialAccount("Instagram", false, "", "Creator/Business via Meta login (1h signed link)", "https://your-domain/oauth/instagram/callback"),
            SocialAccount("TikTok", false, "", "Direct Post API (private until audited)", "https://your-domain/oauth/tiktok/callback")
        )
    }

    val postQueue = remember {
        mutableStateListOf(
            QueuedPost("post-1", "clip-101", "Why Self-Hosted AI Beats Subscriptions #shorts", "YouTube", "Scheduled (4h spacing)", "Today, 23:00")
        )
    }

    val usageRows = remember {
        mutableStateListOf(
            AiUsageRow("Building Open Source Tools in 2026", "gemini / gemini-2.5-flash", 4, 18420, 1420, 4),
            AiUsageRow("Twitch VOD - Ranked Marathon", "gemini / gemini-2.5-flash", 3, 12100, 980, 3),
            AiUsageRow("Live Stream Buffer #live-1", "gemini / gemini-2.5-flash", 6, 9400, 710, 2)
        )
    }

    val adminUsers = remember {
        mutableStateListOf(
            AdminUser("owner@localhost", true, 14),
            AdminUser("editor@studio.local", false, 5)
        )
    }

    // 1-second live telemetry & rolling buffer ticker
    var cpuLoad by remember { mutableStateOf(24) }
    var ramLoad by remember { mutableStateOf(41) }
    val diskFreeGb = 124

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            cpuLoad = (20 + activeJobs.size * 15 + (0..8).random()).coerceAtMost(99)
            ramLoad = 40 + (0..5).random()
            liveSessions.forEachIndexed { idx, s ->
                liveSessions[idx] = s.copy(
                    uptimeSec = s.uptimeSec + 1,
                    loudnessDb = -18 + (0..9).random(),
                    chatPerMin = 32 + (0..30).random()
                )
            }
        }
    }

    fun enqueueVideoJob(sourceTitle: String, uri: Uri? = null, wsId: String = workspaces.first().id, style: CaptionStyle = CaptionStyle.ANTON) {
        val job = PipelineJob("job-${System.currentTimeMillis()}", sourceTitle, "Downloading / Probing", 0.15f)
        activeJobs.add(0, job)
        scope.launch {
            val stages = listOf(
                "Transcribing (Parakeet ONNX)" to 0.45f,
                "AI Picking Moments" to 0.75f,
                "Rendering 9:16 + Captions" to 0.95f
            )
            for ((stageName, prog) in stages) {
                delay(700)
                val idx = activeJobs.indexOfFirst { it.id == job.id }
                if (idx >= 0) activeJobs[idx] = activeJobs[idx].copy(stage = stageName, progress = prog)
            }
            delay(600)
            activeJobs.removeAll { it.id == job.id }
            val newClip = ClipItem(
                id = "clip-${System.currentTimeMillis()}",
                workspaceId = wsId,
                title = "${sourceTitle.take(28)} — Highlight",
                hook = "Wait until you hear this part...",
                sourceTitle = sourceTitle,
                durationSec = 36,
                style = style,
                score = 90,
                transcript = "Once we moved speech transcription onto the local Parakeet model and paired it with automated 9:16 framing, clip output tripled.",
                localUri = uri
            )
            clips.add(0, newClip)
            selectedClipId = newClip.id
            usageRows.add(
                0,
                AiUsageRow(
                    source = sourceTitle.take(32),
                    providerModel = if (aiProvider == "none") "Loudness-only" else "$aiProvider / ${aiModels.substringBefore(",")}",
                    requests = if (aiProvider == "none") 0 else 4,
                    tokensIn = if (aiProvider == "none") 0 else 14200,
                    tokensOut = if (aiProvider == "none") 0 else 1150,
                    clipsFound = 1
                )
            )
            snackbarHostState.showSnackbar("Clip ready: ${newClip.title}")
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        uris.take(5).forEachIndexed { idx, uri ->
            enqueueVideoJob("Uploaded_Video_${idx + 1}.mp4", uri)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF97316)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("PC", color = Color(0xFF0C0E12), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Column {
                            Text("PeakCut", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("CPU $cpuLoad% · RAM $ramLoad% · ${diskFreeGb}GB free", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                    }
                },
                actions = {
                    AssistChip(
                        onClick = { showUpdateDialog = true },
                        label = { Text("Update", fontSize = 11.sp) }
                    )
                    Spacer(Modifier.width(6.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF13161D))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF13161D)) {
                NavigationBarItem(
                    selected = currentTab == "studio",
                    onClick = { currentTab = "studio" },
                    icon = { Icon(Icons.Default.Movie, contentDescription = null) },
                    label = { Text("Studio", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == "live",
                    onClick = { currentTab = "live" },
                    icon = { Icon(Icons.Default.Sensors, contentDescription = null) },
                    label = { Text("Live (${liveSessions.size})", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == "workspaces",
                    onClick = { currentTab = "workspaces" },
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    label = { Text("Creators", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == "posting",
                    onClick = { currentTab = "posting" },
                    icon = { Icon(Icons.Default.ScheduleSend, contentDescription = null) },
                    label = { Text("Post (${postQueue.size})", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == "settings",
                    onClick = { currentTab = "settings" },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("AI & Admin", fontSize = 10.sp) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (currentTab) {
                "studio" -> StudioScreen(
                    clips = clips,
                    selectedClipId = selectedClipId,
                    onSelectClip = { selectedClipId = it },
                    activeJobs = activeJobs,
                    onSubmitUrl = { url, style -> enqueueVideoJob(url, null, workspaces.first().id, style) },
                    onPickFiles = {
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                    onQueuePost = { clip ->
                        postQueue.add(0, QueuedPost("post-${System.currentTimeMillis()}", clip.id, clip.title, "YouTube", "Queued (Spacing respected)", "Next slot"))
                        scope.launch { snackbarHostState.showSnackbar("Queued '${clip.title}'") }
                    }
                )
                "live" -> LiveClippingScreen(
                    sessions = liveSessions,
                    onStartLive = { url ->
                        if (liveSessions.size < 2) {
                            liveSessions.add(LiveSession("live-${System.currentTimeMillis()}", url, workspaces.first().id, 5, -15, 36, 0, "Stream buffer connected..."))
                        }
                    },
                    onStopLive = { id -> liveSessions.removeAll { it.id == id } },
                    onCaptureLive = { session ->
                        session.clipsCaptured++
                        clips.add(
                            0,
                            ClipItem(
                                id = "clip-${System.currentTimeMillis()}",
                                workspaceId = session.workspaceId,
                                title = "Live Highlight #${session.clipsCaptured}",
                                hook = "Caught live on stream!",
                                sourceTitle = session.url,
                                durationSec = 38,
                                style = CaptionStyle.ANTON,
                                score = 92,
                                transcript = session.transcriptTail + " That play was unreal!"
                            )
                        )
                        scope.launch { snackbarHostState.showSnackbar("Live highlight added to Studio") }
                    }
                )
                "workspaces" -> WorkspacesScreen(workspaces = workspaces)
                "posting" -> PostQueueScreen(accounts = socialAccounts, queue = postQueue)
                "settings" -> SettingsAndUsageScreen(
                    aiProvider = aiProvider,
                    onProviderChange = {
                        aiProvider = it
                        providerModels[it]?.firstOrNull()?.let { m -> aiModels = m }
                    },
                    aiModels = aiModels,
                    onModelsChange = { aiModels = it },
                    aiKey = aiKey,
                    onKeyChange = { aiKey = it },
                    aiBaseUrl = aiBaseUrl,
                    onBaseUrlChange = { aiBaseUrl = it },
                    availableModels = providerModels[aiProvider] ?: emptyList(),
                    usageRows = usageRows,
                    accountsEnabled = accountsEnabled,
                    onToggleAccounts = { accountsEnabled = it },
                    signedInEmail = signedInEmail,
                    adminUsers = adminUsers
                )
            }
        }
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("PeakCut Update Check") },
            text = {
                Text("PeakCut checks GitHub every few hours (PEAKCUT_UPDATE_CHECK=1). Run `git pull && pip install -r requirements.txt` on your host server. Your .env, AI keys, and ./data clips stay untouched.")
            },
            confirmButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("Got it") }
            }
        )
    }
}

@Composable
fun StudioScreen(
    clips: MutableList<ClipItem>,
    selectedClipId: String,
    onSelectClip: (String) -> Unit,
    activeJobs: List<PipelineJob>,
    onSubmitUrl: (String, CaptionStyle) -> Unit,
    onPickFiles: () -> Unit,
    onQueuePost: (ClipItem) -> Unit
) {
    var urlInput by remember { mutableStateOf("") }
    var chosenStyle by remember { mutableStateOf(CaptionStyle.ANTON) }
    val selectedClip = clips.find { it.id == selectedClipId } ?: clips.firstOrNull()

    var isPlaying by remember { mutableStateOf(false) }
    var playheadSec by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying, selectedClip?.id) {
        while (isPlaying && selectedClip != null) {
            delay(250)
            playheadSec += 0.25f
            if (playheadSec >= selectedClip.durationSec) playheadSec = 0f
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF13161D))) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Clip a Video (YouTube, Twitch, Kick or Up to 5 Files)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = { Text("https://youtube.com/watch?v=...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (urlInput.isNotBlank()) {
                                    onSubmitUrl(urlInput.trim(), chosenStyle)
                                    urlInput = ""
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Find & Clip")
                        }
                        OutlinedButton(onClick = onPickFiles, modifier = Modifier.weight(1f)) {
                            Text("Upload Videos (5 max)")
                        }
                    }
                }
            }
        }

        if (activeJobs.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF191D26))) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Fair Queue (${activeJobs.size} running)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        activeJobs.forEach { job ->
                            Text("${job.title} — ${job.stage}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            LinearProgressIndicator(progress = { job.progress }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        if (selectedClip != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF13161D))) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("9:16 Vertical Preview (${selectedClip.style.name})", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            TextButton(onClick = {
                                val next = CaptionStyle.entries[(selectedClip.style.ordinal + 1) % CaptionStyle.entries.size]
                                val idx = clips.indexOfFirst { it.id == selectedClip.id }
                                if (idx >= 0) clips[idx] = selectedClip.copy(style = next)
                            }) {
                                Text("Switch Style")
                            }
                        }

                        // 9:16 Phone Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF07090D))
                                .border(1.dp, Color(0xFF252A36), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            if (selectedClip.hook.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(selectedClip.hook, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                                }
                            }

                            val words = selectedClip.transcript.split(" ").filter { it.isNotBlank() }
                            val activeWordIdx = if (words.isEmpty()) 0 else ((playheadSec / selectedClip.durationSec) * words.size).toInt().coerceIn(0, words.lastIndex)
                            val chunkStart = (activeWordIdx / 3) * 3
                            val chunk = words.subList(chunkStart, (chunkStart + 3).coerceAtMost(words.size))

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 24.dp)
                                    .background(
                                        when (selectedClip.style) {
                                            CaptionStyle.ANTON -> Color.Transparent
                                            CaptionStyle.ARCHIVO -> Color.Black.copy(alpha = 0.85f)
                                            CaptionStyle.POPPINS -> Color.White
                                        },
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = chunk.joinToString(" "),
                                    color = if (selectedClip.style == CaptionStyle.POPPINS) Color(0xFFEA580C) else Color(0xFFFACC15),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { isPlaying = !isPlaying }) {
                                Text(if (isPlaying) "Pause" else "Play")
                            }
                            Slider(
                                value = playheadSec,
                                onValueChange = { playheadSec = it },
                                valueRange = 0f..selectedClip.durationSec.toFloat(),
                                modifier = Modifier.weight(1f)
                            )
                            Text("${playheadSec.toInt()}s/${selectedClip.durationSec}s", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        OutlinedTextField(
                            value = selectedClip.hook,
                            onValueChange = { newHook ->
                                val idx = clips.indexOfFirst { it.id == selectedClip.id }
                                if (idx >= 0) clips[idx] = selectedClip.copy(hook = newHook)
                            },
                            label = { Text("Hook Overlay") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = selectedClip.transcript,
                            onValueChange = { newTx ->
                                val idx = clips.indexOfFirst { it.id == selectedClip.id }
                                if (idx >= 0) clips[idx] = selectedClip.copy(transcript = newTx)
                            },
                            label = { Text("Word-by-Word Captions") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(onClick = { onQueuePost(selectedClip) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Queue Clip to Connected Accounts")
                        }
                    }
                }
            }
        }

        items(clips, key = { it.id }) { clip ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelectClip(clip.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (clip.id == selectedClip?.id) Color(0xFF1F2430) else Color(0xFF13161D)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(clip.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${clip.sourceTitle} · ${clip.durationSec}s · Score ${clip.score}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                    IconButton(onClick = { clips.remove(clip) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun LiveClippingScreen(
    sessions: List<LiveSession>,
    onStartLive: (String) -> Unit,
    onStopLive: (String) -> Unit,
    onCaptureLive: (LiveSession) -> Unit
) {
    var streamUrl by remember { mutableStateOf("") }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF13161D))) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Live Stream Rolling Buffer", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Records Twitch, Kick, or YouTube streams and clips highlights using loudness, chat activity, and transcript.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    OutlinedTextField(
                        value = streamUrl,
                        onValueChange = { streamUrl = it },
                        placeholder = { Text("https://twitch.tv/creator or https://kick.com/creator") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (streamUrl.isNotBlank()) {
                                onStartLive(streamUrl.trim())
                                streamUrl = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Live Recording")
                    }
                }
            }
        }

        items(sessions, key = { it.id }) { s ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF13161D))) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("LIVE · ${s.url}", fontWeight = FontWeight.Bold, color = Color(0xFFFCA5A5), fontSize = 13.sp)
                        TextButton(onClick = { onStopLive(s.id) }) { Text("Stop") }
                    }
                    Text("Loudness: ${s.loudnessDb} dBFS · Chat: ${s.chatPerMin}/min · Clips: ${s.clipsCaptured}", fontSize = 12.sp)
                    Text("Transcript: ${s.transcriptTail}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Button(onClick = { onCaptureLive(s) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Clip Last 45s Now")
                    }
                }
            }
        }
    }
}

@Composable
fun WorkspacesScreen(workspaces: MutableList<CreatorWorkspace>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Button(
                onClick = {
                    workspaces.add(
                        CreatorWorkspace(
                            id = "ws-${System.currentTimeMillis()}",
                            name = "Creator ${workspaces.size + 1}",
                            channelUrl = "https://youtube.com/@creator",
                            platform = "YouTube",
                            defaultStyle = CaptionStyle.POPPINS,
                            autoPost = false,
                            spacingHours = 4,
                            dailyCap = 3,
                            logs = mutableListOf("Workspace initialized")
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ New Creator Workspace")
            }
        }
        items(workspaces, key = { it.id }) { ws ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF13161D))) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${ws.name} (${ws.platform})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Channel: ${ws.channelUrl}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text("Style: ${ws.defaultStyle.label} · Spacing: ${ws.spacingHours}h · Daily Cap: ${ws.dailyCap}", fontSize = 11.sp)
                    ws.logs.forEach { log ->
                        Text("• $log", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }
}

@Composable
fun PostQueueScreen(accounts: MutableList<SocialAccount>, queue: MutableList<QueuedPost>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF13161D))) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("One-Click Connected Apps", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    accounts.forEachIndexed { idx, acc ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${acc.platform} (${if (acc.connected) "Connected" else "Disconnected"})", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                Text(acc.auditNote, fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
                            OutlinedButton(onClick = {
                                accounts[idx] = acc.copy(connected = !acc.connected, handle = if (!acc.connected) "@creator_${acc.platform.lowercase()}" else "")
                            }) {
                                Text(if (acc.connected) "Disconnect" else "Connect", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        items(queue, key = { it.id }) { item ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF13161D))) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${item.platform}: ${item.title}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("${item.status} · ${item.window}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                    Button(onClick = {
                        val idx = queue.indexOf(item)
                        if (idx >= 0) queue[idx] = item.copy(status = "Posted")
                    }) {
                        Text("Post Now", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsAndUsageScreen(
    aiProvider: String,
    onProviderChange: (String) -> Unit,
    aiModels: String,
    onModelsChange: (String) -> Unit,
    aiKey: String,
    onKeyChange: (String) -> Unit,
    aiBaseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    availableModels: List<String>,
    usageRows: List<AiUsageRow>,
    accountsEnabled: Boolean,
    onToggleAccounts: (Boolean) -> Unit,
    signedInEmail: String,
    adminUsers: MutableList<AdminUser>
) {
    val providers = listOf("gemini", "openai", "xai", "groq", "openrouter", "anthropic", "ollama", "custom", "none")

    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF13161D))) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose Your AI (BYOK or Loudness-only)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        providers.take(5).forEach { p ->
                            FilterChip(selected = aiProvider == p, onClick = { onProviderChange(p) }, label = { Text(p, fontSize = 10.sp) })
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        providers.drop(5).forEach { p ->
                            FilterChip(selected = aiProvider == p, onClick = { onProviderChange(p) }, label = { Text(p, fontSize = 10.sp) })
                        }
                    }
                    OutlinedTextField(
                        value = aiModels,
                        onValueChange = onModelsChange,
                        label = { Text("PEAKCUT_AI_MODELS (comma-separated fallback)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (availableModels.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            availableModels.forEach { m ->
                                AssistChip(onClick = { onModelsChange(m) }, label = { Text(m, fontSize = 10.sp) })
                            }
                        }
                    }
                    if (aiProvider != "ollama" && aiProvider != "none") {
                        OutlinedTextField(
                            value = aiKey,
                            onValueChange = onKeyChange,
                            label = { Text("PEAKCUT_AI_KEY (stored locally)") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    if (aiProvider == "custom" || aiProvider == "ollama") {
                        OutlinedTextField(
                            value = aiBaseUrl,
                            onValueChange = onBaseUrlChange,
                            label = { Text("PEAKCUT_AI_BASE_URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF13161D))) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("AI Usage Per Video (Local Parakeet STT = \$0)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    usageRows.forEach { r ->
                        Text("${r.source} · ${r.providerModel} · ${r.requests} reqs (${r.tokensIn} in / ${r.tokensOut} out)", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF13161D))) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Multi-User Accounts (PEAKCUT_ACCOUNTS=1)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Switch(checked = accountsEnabled, onCheckedChange = onToggleAccounts)
                    }
                    if (accountsEnabled) {
                        Text("Signed in: $signedInEmail · Admin Dashboard (/admin)", fontSize = 11.sp, color = Color(0xFF86EFAC))
                        adminUsers.forEachIndexed { idx, u ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${u.email} (${if (u.isAdmin) "admin" else "user"})", fontSize = 12.sp)
                                OutlinedButton(onClick = { adminUsers[idx] = u.copy(isAdmin = !u.isAdmin) }) {
                                    Text(if (u.isAdmin) "Revoke" else "Grant", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
