package com.waya0125.wayaloader

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDL.UpdateChannel
import com.yausername.ffmpeg.FFmpeg
import com.yausername.aria2c.Aria2c
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle shared intent (URL sharing from other apps)
        val initialUrl = if (Intent.ACTION_SEND == intent.action && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        } else {
            ""
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(initialUrl)
                }
            }
        }
    }
}

@Composable
fun MainScreen(initialUrl: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var url by remember { mutableStateOf(initialUrl) }
    var logs by remember { mutableStateOf("Ready.\n") }
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    
    // Formats (WAV restored)
    val formats = listOf("mp4", "mkv", "mp3", "m4a", "wav")
    var selectedFormat by remember { mutableStateOf("mp4") }
    val isAudio = selectedFormat in listOf("mp3", "m4a", "wav")

    // Scroll state for logs
    val logScrollState = rememberScrollState()

    // Auto-scroll logs
    LaunchedEffect(logs) {
        logScrollState.animateScrollTo(logScrollState.maxValue)
    }

    // Init yt-dlp, FFmpeg, Aria2c
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                YoutubeDL.getInstance().init(context)
                logs += "yt-dlp initialized.\n"
            } catch (e: Exception) {
                logs += "yt-dlp Init failed: ${e.localizedMessage}\n"
                Log.e("Wayaloader", "yt-dlp init failed", e)
            }
            
            try {
                FFmpeg.getInstance().init(context)
                logs += "FFmpeg initialized.\n"
            } catch (e: Exception) {
                logs += "FFmpeg Init failed: ${e.localizedMessage}\n"
                Log.e("Wayaloader", "FFmpeg init failed", e)
            }

            try {
                Aria2c.getInstance().init(context)
                logs += "Aria2c initialized.\n"
            } catch (e: Exception) {
                logs += "Aria2c Init failed: ${e.localizedMessage}\n"
                Log.e("Wayaloader", "Aria2c init failed", e)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)

        // URL Input with Clear Button
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text(stringResource(R.string.url_hint)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            singleLine = true,
            trailingIcon = {
                if (url.isNotEmpty()) {
                    IconButton(onClick = { url = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear URL")
                    }
                }
            }
        )

        // Format Selection
        Text(text = stringResource(R.string.label_format), style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Simple split for display (Video / Audio)
            Column {
                Text("Video", style = MaterialTheme.typography.labelSmall)
                formats.filter { it in listOf("mp4", "mkv") }.forEach { fmt ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedFormat == fmt, onClick = { selectedFormat = fmt })
                        Text(fmt)
                    }
                }
            }
            Column {
                Text("Audio", style = MaterialTheme.typography.labelSmall)
                formats.filter { it !in listOf("mp4", "mkv") }.forEach { fmt ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedFormat == fmt, onClick = { selectedFormat = fmt })
                        Text(fmt)
                    }
                }
            }
        }

        // Progress Bar
        if (isDownloading) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (url.isNotBlank() && !isDownloading) {
                        isDownloading = true
                        logs += "Starting download...\n"
                        scope.launch {
                            downloadContent(context, url, selectedFormat, isAudio) { msg, prog ->
                                if (msg != null) logs += "$msg\n"
                                if (prog != null) progress = prog / 100f
                            }
                            isDownloading = false
                            progress = 0f
                        }
                    }
                },
                enabled = !isDownloading,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.btn_download))
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        logs += "Updating yt-dlp...\n"
                        try {
                            withContext(Dispatchers.IO) {
                                YoutubeDL.getInstance().updateYoutubeDL(context, UpdateChannel.STABLE)
                            }
                            logs += "Update successful!\n"
                        } catch (e: Exception) {
                            logs += "Update failed: ${e.message}\n"
                        }
                    }
                },
                enabled = !isDownloading
            ) {
                Text(stringResource(R.string.btn_update_ytdlp))
            }
        }

        // Logs
        Text(text = stringResource(R.string.label_logs), style = MaterialTheme.typography.titleMedium)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = logs,
                modifier = Modifier
                    .padding(8.dp)
                    .verticalScroll(logScrollState),
                fontSize = 12.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

suspend fun downloadContent(
    context: Context,
    url: String,
    format: String,
    isAudio: Boolean,
    callback: (String?, Float?) -> Unit
) {
    withContext(Dispatchers.IO) {
        val appDir = File(context.filesDir, "downloader_temp")
        if (!appDir.exists()) appDir.mkdirs()

        val request = YoutubeDLRequest(url)
        request.addOption("-o", "${appDir.absolutePath}/%(title)s.%(ext)s")
        
        // Aria2c optimization
        request.addOption("--downloader", "libaria2c.so")
        request.addOption("--external-downloader-args", "aria2c:-x 16 -k 1M")
        
        if (isAudio) {
            request.addOption("--extract-audio")
            request.addOption("--audio-format", format)
            
            // Thumbnail embedding only for supported formats
            if (format != "wav") {
                request.addOption("--embed-thumbnail")
            }
        } else {
            // Video optimization: try to find native format first to avoid muxing
            if (format == "mp4") {
                 request.addOption("--format", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best")
            } else {
                 request.addOption("--format", "bestvideo+bestaudio/best")
            }
            request.addOption("--merge-output-format", format)
        }

        try {
            YoutubeDL.getInstance().execute(request) { progress, etaInSeconds, line ->
                callback(null, progress)
            }
            callback("Download finished. Moving file...", 100f)

            // Move file to Downloads
            val downloadedFile = appDir.listFiles()?.firstOrNull()
            if (downloadedFile != null) {
                moveToDownloads(context, downloadedFile)
                downloadedFile.delete() // Clean up temp
                callback("Saved to Downloads/Wayaloader", null)
            } else {
                callback("Error: File not found after download.", null)
            }

        } catch (e: Exception) {
            callback("Error: ${e.message}", null)
            e.printStackTrace()
        }
    }
}

fun moveToDownloads(context: Context, file: File) {
    val filename = file.name
    val mimeType = when (file.extension.lowercase()) {
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "wav" -> "audio/wav"
        "ogg" -> "audio/ogg"
        "mp4" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        else -> "*/*"
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Wayaloader")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            resolver.openOutputStream(it)?.use { output ->
                file.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
        }
    } else {
        // Legacy storage (Android 9)
        val targetDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Wayaloader")
        if (!targetDir.exists()) targetDir.mkdirs()
        val targetFile = File(targetDir, filename)
        file.copyTo(targetFile, overwrite = true)
    }
}
