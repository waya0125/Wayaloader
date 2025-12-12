# AI Development Guide for Wayaloader

## Technical Specifications

*   **Package Name**: `com.waya0125.wayaloader`
*   **Version**: 2025.12.12
*   **Language**: Kotlin (Jetpack Compose)
*   **Build System**: Gradle (Kotlin DSL)
*   **Target SDK**: 34
*   **Min SDK**: 28 (Android 9.0)

## Core Libraries

*   **yt-dlp Wrapper**: `io.github.junkfood02.youtubedl-android` (Version 0.18.1)
    *   *Note*: This fork is used for better Maven Central availability and Aria2c support.
*   **Aria2c**: Used as an external downloader for yt-dlp to improve speed.
*   **FFmpeg**: Used for audio extraction and muxing.

## Key Configurations

*   **Manifest**:
    *   `android:extractNativeLibs="true"` is **MANDATORY** for the python/yt-dlp binaries to run.
    *   `android:requestLegacyExternalStorage="true"` for Android 10 support.
*   **Gradle**:
    *   `abiFilters "arm64-v8a"`: Restricts architecture to ARM64 to prevent `OutOfMemoryError` during build (APK size optimization).
    *   `android.packaging.jniLibs.useLegacyPackaging = true`: Required by AGP when `extractNativeLibs` is true.
    *   `jvmargs`: Set to `-Xmx4g` in `gradle.properties` to handle large resource merging.

## CI/CD

*   **GitHub Actions**: Configured in `.github/workflows/android.yml`. Builds debug APK on push.
*   **Dependabot**: Configured in `.github/dependabot.yml` to monitor Gradle dependencies.

## Troubleshooting

*   **Init Failed**: Ensure `extractNativeLibs="true"` is set.
*   **Build OOM**: Ensure only `arm64-v8a` is targeted and heap size is increased.
*   **Slow Download**: `aria2c` is configured with `-x 16` arguments. MP4 downloads try to find native formats first to avoid ffmpeg muxing overhead.
