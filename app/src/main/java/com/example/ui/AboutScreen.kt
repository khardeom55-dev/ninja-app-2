package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ABOUT THIS EDITOR",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            letterSpacing = 1.sp
                        ),
                        color = NinjaTextWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NinjaTextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NinjaDarkBackground
                )
            )
        },
        containerColor = NinjaDarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NinjaDarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NinjaRedPrimary)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = NinjaRedGlow, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "NINJA AUTO EDITOR",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic
                                ),
                                color = NinjaTextWhite
                            )
                        }

                        Text(
                            text = "Ninja Auto Editor is a high-performance gameplay clip generator tailored for esports creators like King Ninja55.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NinjaTextMuted
                        )
                    }
                }
            }

            item {
                AboutFeatureItem(
                    icon = Icons.Default.OfflineBolt,
                    title = "100% Offline Local Engine",
                    description = "Highlight selection and export operate strictly on your device hardware without requiring paid cloud APIs, active internet subscriptions, or external servers."
                )
            }

            item {
                AboutFeatureItem(
                    icon = Icons.Default.Analytics,
                    title = "Automatic Signal Heuristics",
                    description = "Key moments are discovered by analyzing audio loudness peaks, sound intensity spikes, scene-change color shifts, and frame motion variance to rank epic gameplay clips."
                )
            }

            item {
                AboutFeatureItem(
                    icon = Icons.Default.Memory,
                    title = "Local Hardware Encoding",
                    description = "Video composition utilizes native Android MediaCodec and Media3 Transformer hardware pipelines to output clean 1080p and 720p Shorts and Long Edits."
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NinjaDarkSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NinjaCardOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "DISCLAIMER",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = NinjaRedGlow
                        )
                        Text(
                            text = "Heuristic highlight detection is automated. While it accurately ranks high-energy gameplay bursts, clips may occasionally benefit from manual trim adjustment.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NinjaTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutFeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NinjaDarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NinjaCardOutline)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(NinjaRedPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = NinjaRedGlow, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = NinjaTextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = NinjaTextMuted
                )
            }
        }
    }
}
