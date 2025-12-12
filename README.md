# Wayaloader

**Wayaloader** is a powerful YouTube and SoundCloud downloader for Android, powered by [yt-dlp](https://github.com/yt-dlp/yt-dlp).  
Powered by Gemini 3 Pro.

## Features

*   **Supported Sites**: YouTube, SoundCloud, and many others supported by yt-dlp.
*   **Video Formats**: MP4, MKV (Auto-merge best video/audio).
*   **Audio Formats**: MP3, M4A, WAV (Audio extraction).
*   **Thumbnails**: Embeds thumbnails into audio files (except WAV).
*   **High Performance**: Uses `aria2c` for multi-threaded downloading.
*   **Auto Updates**: Features an in-app button to update the internal `yt-dlp` binary to the latest version.
*   **Shared Intent**: Supports sharing URLs directly from the YouTube app.

## Requirements

*   **Android**: 9.0 (Pie) or higher.
*   **Architecture**: ARM64 (arm64-v8a) only (optimized for size).

## Usage

1.  Paste a URL or share it from another app.
2.  Select the desired format.
3.  Tap **Download**.
4.  The file will be saved to your device's `Downloads/Wayaloader` folder.

## Build & Install

### Prerequisites
*   JDK 17
*   Android SDK (API 34)

### Build Command
```bash
./gradlew assembleDebug
```

## Credits
*   **Core**: [youtubedl-android](https://github.com/junkfood02/youtubedl-android) (JunkFood02 Fork)
*   **Downloader**: yt-dlp, aria2c, FFmpeg
*   **UI**: Jetpack Compose
