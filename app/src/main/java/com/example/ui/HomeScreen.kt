package com.example.ui

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.*
import com.example.ui.theme.*
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun HomeScreen(
    viewModel: EditorViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToMemes: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {}
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selectedUri by viewModel.selectedVideoUri.collectAsStateWithLifecycle()
    val selectedMetadata by viewModel.selectedVideoMetadata.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()

    var showLargeVideoWarning by remember { mutableStateOf(false) }

    LaunchedEffect(selectedMetadata) {
        selectedMetadata?.let { meta ->
            if (meta.width > 1920 || meta.height > 1920 || meta.durationMs > 1800000L) {
                showLargeVideoWarning = true
            }
        }
    }

    if (showLargeVideoWarning) {
        AlertDialog(
            onDismissRequest = { showLargeVideoWarning = false },
            title = {
                Text(
                    text = "LARGE / 4K VIDEO DETECTED",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NinjaRedGlow
                )
            },
            text = {
                Text(
                    text = "High-resolution video processing requires hardware encoding. Mobile rendering may take additional time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NinjaTextWhite
                )
            },
            confirmButton = {
                Button(
                    onClick = { showLargeVideoWarning = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NinjaRedPrimary)
                ) {
                    Text("CONTINUE", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = NinjaDarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectVideo(it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NinjaDarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Top Header Card with Settings Icon
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("header_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NinjaDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NinjaCardOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "NINJA AUTO EDITOR",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                letterSpacing = (-0.5).sp
                            ),
                            color = NinjaRedPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "CHANNEL: ${settings.channelName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = NinjaTextSubtle
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = onNavigateToHistory, modifier = Modifier.testTag("history_btn")) {
                            Icon(Icons.Default.History, contentDescription = "History", tint = NinjaTextWhite)
                        }
                        IconButton(onClick = onNavigateToSettings, modifier = Modifier.testTag("settings_btn")) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = NinjaRedGlow, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }

        // Active Project Export Progress Card
        activeProject?.let { project ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_project_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NinjaDarkSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NinjaRedPrimary)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EDITING: ${project.title}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = NinjaTextWhite
                            )
                            Text(
                                text = "${project.progressPercent}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = NinjaRedGlow
                            )
                        }

                        LinearProgressIndicator(
                            progress = { project.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = NinjaRedPrimary,
                            trackColor = NinjaDarkBackground
                        )

                        Text(
                            text = when (project.status) {
                                ExportStatus.QUEUED -> "Queued for local processing..."
                                ExportStatus.PROCESSING -> "Detecting highlights, cropping & exporting MP4..."
                                ExportStatus.COMPLETED -> "Export Completed! Saved to ${settings.outputFolder}"
                                ExportStatus.FAILED -> "Export Failed: ${project.errorMessage}"
                                ExportStatus.CANCELLED -> "Export Cancelled"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (project.status == ExportStatus.FAILED) Color.Red else NinjaTextMuted
                        )

                        if (project.status == ExportStatus.COMPLETED && project.outputFilePath != null) {
                            val outputFile = File(project.outputFilePath)
                            if (outputFile.exists()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                KeyVideoPreviewPlayer(filePath = project.outputFilePath)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                outputFile
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "video/mp4"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Ninja Video"))
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = NinjaRedPrimary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SHARE", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. SELECT GAMEPLAY VIDEO CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { videoPickerLauncher.launch("video/*") }
                    .testTag("select_video_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NinjaDarkSurface),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (selectedUri != null) NinjaRedPrimary.copy(alpha = 0.8f) else NinjaCardOutline
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(NinjaDarkSurface)
                ) {
                    if (selectedUri == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(NinjaRedPrimary.copy(alpha = 0.15f))
                                    .border(1.dp, NinjaRedGlow.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoCall,
                                    contentDescription = "Select Gameplay Video",
                                    tint = NinjaRedPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "SELECT GAMEPLAY VIDEO",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = NinjaTextWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap to choose a gameplay MP4 from your device",
                                style = MaterialTheme.typography.bodySmall,
                                color = NinjaTextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Selected Video Display Card
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val durSec = (selectedMetadata?.durationMs ?: 0L) / 1000
                                    val durMin = durSec / 60
                                    val remSec = durSec % 60
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, NinjaCardOutline)
                                    ) {
                                        Text(
                                            text = String.format("%02d:%02d", durMin, remSec),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = NinjaTextWhite,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = { videoPickerLauncher.launch("video/*") },
                                    colors = ButtonDefaults.textButtonColors(contentColor = NinjaRedGlow)
                                ) {
                                    Text("CHANGE VIDEO", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .align(Alignment.CenterHorizontally)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = NinjaTextWhite,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(NinjaSuccessGreen)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = selectedMetadata?.fileName ?: "Gameplay.mp4",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = NinjaTextWhite
                                    )
                                }

                                if (isAnalyzing) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = NinjaRedPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Analyzing...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NinjaTextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. START EDIT ACTION BUTTON
        item {
            Button(
                onClick = { viewModel.startAutoEdit() },
                enabled = selectedUri != null && !isAnalyzing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("start_edit_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NinjaRedPrimary,
                    disabledContainerColor = NinjaDarkSurfaceVariant
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "Start Edit",
                        tint = if (selectedUri != null) NinjaTextWhite else NinjaTextSubtle,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "START EDIT",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = if (selectedUri != null) NinjaTextWhite else NinjaTextSubtle
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun KeyVideoPreviewPlayer(filePath: String) {
    val context = LocalContext.current
    val exoPlayer = remember(filePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(filePath))))
            prepare()
        }
    }

    DisposableEffect(filePath) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    600
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, NinjaCardOutline, RoundedCornerShape(8.dp))
    )
}
