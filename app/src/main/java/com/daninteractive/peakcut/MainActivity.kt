package com.daninteractive.peakcut

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class CaptionStyle(val label: String) {
    ANTON("Style 1: Anton Bold Pop"),
    ARCHIVO("Style 2: Archivo Box"),
    POPPINS("Style 3: Poppins Clean")
}

enum class AspectPreset(val label: String, val ratio: Float) {
    VERTICAL_9_16("9:16", 9f / 16f),
    FEED_4_5("4:5", 4f / 5f),
    SQUARE_1_1("1:1", 1f)
}

enum class AccentPreset(val label: String, val primary: Color, val container: Color) {
    AMBER("PeakCut Amber", Color(0xFFF97316), Color(0xFF3A1E0A)),
    INDIGO("Material Indigo", Color(0xFF818CF8), Color(0xFF1E224A)),
    EMERALD("Studio Emerald", Color(0xFF34D399), Color(0xFF0E2F24)),
    ROSE("Creator Rose", Color(0xFFFB7185), Color(0xFF3B1520))
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
    var trimStartSec: Float = 0f,
    var trimEndSec: Float = durationSec.toFloat(),
    var aspectPreset: AspectPreset = AspectPreset.VERTICAL_9_16,
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

private val PeakCutTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 13.sp)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var useDynamicColor by remember { mutableStateOf(true) }
            var accentPreset by remember { mutableStateOf(AccentPreset.AMBER) }
            PeakCutTheme(useDynamicColor = useDynamicColor, accentPreset = accentPreset) {
                PeakCutApp(
                    useDynamicColor = useDynamicColor,
                    onToggleDynamicColor = { useDynamicColor = it },
                    accentPreset = accentPreset,
                    onSelectAccent = { accentPreset = it }
                )
            }
        }
    }
}

@Composable
fun PeakCutTheme(
    useDynamicColor: Boolean = true,
    accentPreset: AccentPreset = AccentPreset.AMBER,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        darkColorScheme(
            primary = accentPreset.primary,
            onPrimary = Color(0xFF0C0E12),
            primaryContainer = accentPreset.container,
            onPrimaryContainer = accentPreset.primary,
            background = Color(0xFF0C0E12),
            surface = Color(0xFF13161D),
            surfaceVariant = Color(0xFF1A1E28),
            onBackground = Color(0xFFF1F5F9),
            onSurface = Color(0xFFF1F5F9),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF272D3B)
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = PeakCutTypography,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeakCutApp(
    useDynamicColor: Boolean,
    onToggleDynamicColor: (Boolean) -> Unit,
    accentPreset: AccentPreset,
    onSelectAccent: (AccentPreset) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf("studio") }
    var accountsEnabled by remember { mutableStateOf(false) }
    var signedInEmail by remember { mutableStateOf("owner@localhost") }
    var showUpdateDialog by remember { mutableStateOf(false) }

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
                trimStartSec = 0f,
                trimEndSec = 34f,
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
                trimStartSec = 2f,
                trimEndSec = 24f,
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
                trimStartSec = 0f,
                trimEndSec = 36f,
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("PC", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
                        }
                        Column {
                            Text("PeakCut", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "CPU $cpuLoad% · RAM $ramLoad% · ${diskFreeGb}GB free",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    AssistChip(
                        onClick = { showUpdateDialog = true },
                        label = { Text("Update", style = MaterialTheme.typography.labelMedium) }
                    )
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = currentTab == "studio",
                    onClick = { currentTab = "studio" },
                    icon = { Icon(Icons.Default.Movie, contentDescription = null) },
                    label = { Text("Studio", style = MaterialTheme.typography.labelSmall) }
                )
                NavigationBarItem(
                    selected = currentTab == "live",
                    onClick = { currentTab = "live" },
                    icon = { Icon(Icons.Default.Sensors, contentDescription = null) },
                    label = { Text("Live (${liveSessions.size})", style = MaterialTheme.typography.labelSmall) }
                )
                NavigationBarItem(
                    selected = currentTab == "workspaces",
                    onClick = { currentTab = "workspaces" },
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    label = { Text("Creators", style = MaterialTheme.typography.labelSmall) }
                )
                NavigationBarItem(
                    selected = currentTab == "posting",
                    onClick = { currentTab = "posting" },
                    icon = { Icon(Icons.Default.ScheduleSend, contentDescription = null) },
                    label = { Text("Post (${postQueue.size})", style = MaterialTheme.typography.labelSmall) }
                )
                NavigationBarItem(
                    selected = currentTab == "settings",
                    onClick = { currentTab = "settings" },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("AI & Admin", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = currentTab,
            modifier = Modifier.padding(padding).fillMaxSize(),
            transitionSpec = {
                (fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 28 }) togetherWith fadeOut(tween(120))
            },
            label = "PeakCutTabTransition"
        ) { tab ->
            when (tab) {
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
                                trimStartSec = 0f,
                                trimEndSec = 38f,
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
                    adminUsers = adminUsers,
                    useDynamicColor = useDynamicColor,
                    onToggleDynamicColor = onToggleDynamicColor,
                    accentPreset = accentPreset,
                    onSelectAccent = onSelectAccent
                )
            }
        }
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("PeakCut Update Check", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "PeakCut checks GitHub every few hours (PEAKCUT_UPDATE_CHECK=1). Run `git pull && pip install -r requirements.txt` on your host server. Your .env, AI keys, and ./data clips stay untouched.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("Got it") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var playheadSec by remember(selectedClip?.id) { mutableStateOf(selectedClip?.trimStartSec ?: 0f) }

    LaunchedEffect(isPlaying, selectedClip?.id, selectedClip?.trimStartSec, selectedClip?.trimEndSec) {
        while (isPlaying && selectedClip != null) {
            delay(100)
            playheadSec += 0.1f
            if (playheadSec >= selectedClip.trimEndSec) playheadSec = selectedClip.trimStartSec
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Clip a Video (YouTube, Twitch, Kick or Up to 5 Files)", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = { Text("https://youtube.com/watch?v=...", style = MaterialTheme.typography.bodySmall) },
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
                            Text("Upload Videos (5)")
                        }
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = activeJobs.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Fair Queue (${activeJobs.size} running)", style = MaterialTheme.typography.titleSmall)
                        activeJobs.forEach { job ->
                            val smoothProgress by animateFloatAsState(targetValue = job.progress, animationSpec = tween(250), label = "jobProgress")
                            Text("${job.title} — ${job.stage}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LinearProgressIndicator(progress = { smoothProgress }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        if (selectedClip != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${selectedClip.aspectPreset.label} Preview (${selectedClip.style.name})", style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    val nextAspect = AspectPreset.entries[(selectedClip.aspectPreset.ordinal + 1) % AspectPreset.entries.size]
                                    val idx = clips.indexOfFirst { it.id == selectedClip.id }
                                    if (idx >= 0) clips[idx] = selectedClip.copy(aspectPreset = nextAspect)
                                }) {
                                    Text(selectedClip.aspectPreset.label)
                                }
                                TextButton(onClick = {
                                    val next = CaptionStyle.entries[(selectedClip.style.ordinal + 1) % CaptionStyle.entries.size]
                                    val idx = clips.indexOfFirst { it.id == selectedClip.id }
                                    if (idx >= 0) clips[idx] = selectedClip.copy(style = next)
                                }) {
                                    Text("Style")
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF07090D))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            if (selectedClip.hook.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(selectedClip.hook, color = Color.White, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                                }
                            }

                            val words = selectedClip.transcript.split(" ").filter { it.isNotBlank() }
                            val activeWordIdx = if (words.isEmpty()) 0 else ((playheadSec / selectedClip.durationSec) * words.size).toInt().coerceIn(0, words.lastIndex)
                            val chunkStart = (activeWordIdx / 3) * 3
                            val chunk = words.subList(chunkStart, (chunkStart + 3).coerceAtMost(words.size))

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 22.dp)
                                    .background(
                                        when (selectedClip.style) {
                                            CaptionStyle.ANTON -> Color.Transparent
                                            CaptionStyle.ARCHIVO -> Color.Black.copy(alpha = 0.85f)
                                            CaptionStyle.POPPINS -> Color.White
                                        },
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = chunk.joinToString(" "),
                                    color = if (selectedClip.style == CaptionStyle.POPPINS) MaterialTheme.colorScheme.primary else Color(0xFFFACC15),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Playback & 30fps frame-step controls
                        val frameNum = (playheadSec * 30f).roundToInt()
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { isPlaying = !isPlaying }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(if (isPlaying) "Pause" else "Play", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = {
                                    isPlaying = false
                                    playheadSec = (playheadSec - (1f / 30f)).coerceAtLeast(selectedClip.trimStartSec)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) { Text("-1f", style = MaterialTheme.typography.labelSmall) }
                            OutlinedButton(
                                onClick = {
                                    isPlaying = false
                                    playheadSec = (playheadSec + (1f / 30f)).coerceAtMost(selectedClip.trimEndSec)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) { Text("+1f", style = MaterialTheme.typography.labelSmall) }
                            Slider(
                                value = playheadSec.coerceIn(0f, selectedClip.durationSec.toFloat()),
                                onValueChange = { playheadSec = it },
                                valueRange = 0f..selectedClip.durationSec.toFloat(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            "Playhead: ${String.format("%.2fs", playheadSec)} (Frame #$frameNum @ 30fps) · Trim: ${String.format("%.1fs", selectedClip.trimStartSec)} – ${String.format("%.1fs", selectedClip.trimEndSec)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Frame-accurate Trim In / Trim Out RangeSlider
                        RangeSlider(
                            value = selectedClip.trimStartSec..selectedClip.trimEndSec,
                            onValueChange = { range ->
                                val minGap = 1.0f
                                val start = range.start.coerceIn(0f, (selectedClip.durationSec - minGap).coerceAtLeast(0f))
                                val end = range.endInclusive.coerceIn(start + minGap, selectedClip.durationSec.toFloat())
                                val idx = clips.indexOfFirst { it.id == selectedClip.id }
                                if (idx >= 0) clips[idx] = selectedClip.copy(trimStartSec = start, trimEndSec = end)
                                if (playheadSec < start || playheadSec > end) playheadSec = start
                            },
                            valueRange = 0f..selectedClip.durationSec.toFloat()
                        )

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
                            Text("Queue Trimmed Clip to Connected Accounts")
                        }
                    }
                }
            }
        }

        items(clips, key = { it.id }) { clip ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelectClip(clip.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (clip.id == selectedClip?.id) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(clip.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${clip.sourceTitle} · ${(clip.trimEndSec - clip.trimStartSec).roundToInt()}s · ${clip.aspectPreset.label} · Score ${clip.score}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Live Stream Rolling Buffer", style = MaterialTheme.typography.titleMedium)
                    Text("Records Twitch, Kick, or YouTube streams and clips highlights using loudness, chat activity, and transcript.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("LIVE · ${s.url}", style = MaterialTheme.typography.titleSmall, color = Color(0xFFFCA5A5))
                        TextButton(onClick = { onStopLive(s.id) }) { Text("Stop") }
                    }
                    Text("Loudness: ${s.loudnessDb} dBFS · Chat: ${s.chatPerMin}/min · Clips: ${s.clipsCaptured}", style = MaterialTheme.typography.bodyMedium)
                    Text("Transcript: ${s.transcriptTail}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${ws.name} (${ws.platform})", style = MaterialTheme.typography.titleMedium)
                    Text("Channel: ${ws.channelUrl}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Style: ${ws.defaultStyle.label} · Spacing: ${ws.spacingHours}h · Daily Cap: ${ws.dailyCap}", style = MaterialTheme.typography.bodySmall)
                    ws.logs.forEach { log ->
                        Text("• $log", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("One-Click Connected Apps", style = MaterialTheme.typography.titleMedium)
                    accounts.forEachIndexed { idx, acc ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${acc.platform} (${if (acc.connected) "Connected" else "Disconnected"})", style = MaterialTheme.typography.titleSmall)
                                Text(acc.auditNote, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(onClick = {
                                accounts[idx] = acc.copy(connected = !acc.connected, handle = if (!acc.connected) "@creator_${acc.platform.lowercase()}" else "")
                            }) {
                                Text(if (acc.connected) "Disconnect" else "Connect", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
        items(queue, key = { it.id }) { item ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${item.platform}: ${item.title}", style = MaterialTheme.typography.titleSmall)
                        Text("${item.status} · ${item.window}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = {
                        val idx = queue.indexOf(item)
                        if (idx >= 0) queue[idx] = item.copy(status = "Posted")
                    }) {
                        Text("Post Now", style = MaterialTheme.typography.labelMedium)
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
    adminUsers: MutableList<AdminUser>,
    useDynamicColor: Boolean,
    onToggleDynamicColor: (Boolean) -> Unit,
    accentPreset: AccentPreset,
    onSelectAccent: (AccentPreset) -> Unit
) {
    val providers = listOf("gemini", "openai", "xai", "groq", "openrouter", "anthropic", "ollama", "custom", "none")

    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Material 3 Dynamic Color & Theme", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Android 12+ Material You Wallpaper Colors", style = MaterialTheme.typography.titleSmall)
                            Text("Automatically adapts surfaces & accents on Android 12+", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = useDynamicColor, onCheckedChange = onToggleDynamicColor)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AccentPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = !useDynamicColor && accentPreset == preset,
                                onClick = {
                                    onToggleDynamicColor(false)
                                    onSelectAccent(preset)
                                },
                                label = { Text(preset.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose Your AI (BYOK or Loudness-only)", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        providers.take(5).forEach { p ->
                            FilterChip(selected = aiProvider == p, onClick = { onProviderChange(p) }, label = { Text(p, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        providers.drop(5).forEach { p ->
                            FilterChip(selected = aiProvider == p, onClick = { onProviderChange(p) }, label = { Text(p, style = MaterialTheme.typography.labelSmall) })
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
                                AssistChip(onClick = { onModelsChange(m) }, label = { Text(m, style = MaterialTheme.typography.labelSmall) })
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("AI Usage Per Video (Local Parakeet STT = \$0)", style = MaterialTheme.typography.titleMedium)
                    usageRows.forEach { r ->
                        Text(
                            "${r.source} · ${r.providerModel} · ${r.requests} reqs (${r.tokensIn} in / ${r.tokensOut} out)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Multi-User Accounts (PEAKCUT_ACCOUNTS=1)", style = MaterialTheme.typography.titleSmall)
                        Switch(checked = accountsEnabled, onCheckedChange = onToggleAccounts)
                    }
                    if (accountsEnabled) {
                        Text("Signed in: $signedInEmail · Admin Dashboard (/admin)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF86EFAC))
                        adminUsers.forEachIndexed { idx, u ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${u.email} (${if (u.isAdmin) "admin" else "user"})", style = MaterialTheme.typography.bodyMedium)
                                OutlinedButton(onClick = { adminUsers[idx] = u.copy(isAdmin = !u.isAdmin) }) {
                                    Text(if (u.isAdmin) "Revoke" else "Grant", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
