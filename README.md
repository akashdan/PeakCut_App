# PeakCut Android (Material 3 Jetpack Compose)

Native Android client and mobile clipping studio based on `haziqahmedqasmi/PeakCut`.

## Features Included
1. **Studio & Clips**: Paste YouTube/Twitch/Kick links, pick up to 5 local videos via Android Photo Picker, fair-queue pipeline stages (Download, Parakeet local ONNX speech-to-text, AI moment selection, 9:16 render), and interactive 9:16 vertical player with word-by-word captions in 3 styles (`Anton`, `Archivo Black`, `Poppins`) and hook overlay editor.
2. **Live Clipping**: Rolling live stream buffer for Twitch/Kick/YouTube with real-time loudness (dBFS), chat rate, live transcript tail, and instant 45s highlight capture.
3. **Workspaces**: Per-creator channels, default caption style, auto-post toggle, spacing hours, daily caps, and activity logs.
4. **Post Queue**: One-click OAuth connection management and scheduled/instant posting to YouTube Shorts, Instagram Reels, and TikTok with audit privacy notes.
5. **AI Usage & Settings**: Configure Gemini, OpenAI, Grok (xAI), Groq, OpenRouter, Claude, Ollama, Custom OpenAI URL, or Loudness-only mode; model discovery chips; token/request tracking per video; `PEAKCUT_ACCOUNTS` mode and `/admin` role management.

## Build in Android Studio
1. Open the `PeakCutAndroid` folder in Android Studio Ladybug or newer.
2. Let Gradle sync and click **Run 'app'**.
