package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.example.data.ExportProject
import com.example.data.ExportStatus
import com.example.ui.theme.*
import java.io.File

@Composable
fun HistoryScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PROJECT HISTORY", color = NinjaTextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NinjaTextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NinjaDarkBackground)
            )
        },
        containerColor = NinjaDarkBackground
    ) { padding ->
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = NinjaRedPrimary,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "NO EXPORTED VIDEOS YET",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NinjaTextWhite
                    )
                    Text(
                        text = "Your completed Auto Shorts & Long Videos will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NinjaTextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectHistoryCard(
                        project = project,
                        onShare = {
                            project.outputFilePath?.let { path ->
                                val file = File(path)
                                if (file.exists()) {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "video/mp4"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                                }
                            }
                        },
                        onDelete = { viewModel.deleteProject(project) }
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ProjectHistoryCard(
    project: ExportProject,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedPlayer by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NinjaDarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NinjaCardOutline)
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
                Column {
                    Text(project.title, color = NinjaTextWhite, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Format: ${project.outputType.name}  |  Quality: ${project.quality.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NinjaRedGlow
                    )
                }

                StatusChip(status = project.status)
            }

            if (project.status == ExportStatus.COMPLETED && project.outputFilePath != null) {
                val file = File(project.outputFilePath)
                if (file.exists()) {
                    if (expandedPlayer) {
                        KeyVideoPreviewPlayer(filePath = project.outputFilePath)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { expandedPlayer = !expandedPlayer },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NinjaTextWhite)
                        ) {
                            Icon(if (expandedPlayer) Icons.Default.VisibilityOff else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (expandedPlayer) "CLOSE" else "PREVIEW")
                        }

                        Button(
                            onClick = onShare,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NinjaRedPrimary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SHARE")
                        }

                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NinjaRedGlow)
                        }
                    }
                }
            } else if (project.status == ExportStatus.FAILED) {
                Text(
                    text = project.errorMessage ?: "Failed to process video",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: ExportStatus) {
    val (bgColor, textColor) = when (status) {
        ExportStatus.COMPLETED -> NinjaSuccessGreen to NinjaTextWhite
        ExportStatus.PROCESSING -> NinjaRedPrimary to NinjaTextWhite
        ExportStatus.QUEUED -> NinjaDarkSurfaceVariant to NinjaTextMuted
        ExportStatus.FAILED -> MaterialTheme.colorScheme.error to NinjaTextWhite
        ExportStatus.CANCELLED -> NinjaTextMuted to NinjaTextWhite
    }

    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = status.name,
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
