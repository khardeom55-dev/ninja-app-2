package com.example.ui

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.EditorSettings
import com.example.data.ExportQuality
import com.example.data.MemeFrequency
import com.example.data.WatermarkPosition
import com.example.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun SettingsScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var channelNameState by remember(settings.channelName) { mutableStateOf(settings.channelName) }
    var outputFolderState by remember(settings.outputFolder) { mutableStateOf(settings.outputFolder) }
    
    var gameVolState by remember(settings.gameAudioVolume) { mutableFloatStateOf(settings.gameAudioVolume) }
    var memeVolState by remember(settings.memeAudioVolume) { mutableFloatStateOf(settings.memeAudioVolume) }
    var bgMusicVolState by remember(settings.bgMusicVolume) { mutableFloatStateOf(settings.bgMusicVolume) }
    var watermarkOpacityState by remember(settings.watermarkOpacityPercent) { mutableFloatStateOf(settings.watermarkOpacityPercent.toFloat()) }
    var captionSizeState by remember(settings.captionSizeSp) { mutableFloatStateOf(settings.captionSizeSp.toFloat()) }

    var previewVideoPath by remember { mutableStateOf<String?>(null) }
    var previewVideoTitle by remember { mutableStateOf("") }

    val introPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.importCustomAsset(it, "intros", "Intro Video") }
    }

    val outroPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.importCustomAsset(it, "outros", "Outro Video") }
    }

    val logoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.importCustomAsset(it, "logos", "Channel Logo") }
    }

    val watermarkPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.importCustomAsset(it, "watermarks", "Watermark") }
    }

    val bgMusicPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.importCustomAsset(it, "music", "Background Music") }
    }

    if (previewVideoPath != null) {
        VideoPreviewDialog(
            filePath = previewVideoPath!!,
            title = previewVideoTitle,
            onDismiss = { previewVideoPath = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EDITOR SETTINGS", color = NinjaTextWhite, fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. INTRO & OUTRO SECTION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("intro_outro_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NinjaDarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NinjaCardOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Movie, contentDescription = null, tint = NinjaRedPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "1. INTRO & OUTRO",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = NinjaRedPrimary
                                )
                            }
                        }

                        Divider(color = NinjaCardOutline)

                        // --- INTRO VIDEO ---
                        Text("Intro Video", style = MaterialTheme.typography.labelLarge, color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Intro", color = NinjaTextMuted, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = settings.isIntroEnabled,
                                onCheckedChange = { viewModel.updateSettings(settings.copy(isIntroEnabled = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NinjaRedPrimary, checkedTrackColor = NinjaRedGlow.copy(alpha = 0.3f))
                            )
                        }

                        Text(
                            text = if (settings.introVideoPath != null) "Selected: ${File(settings.introVideoPath!!).name}" else "No intro video selected",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (settings.introVideoPath != null) NinjaRedGlow else NinjaTextMuted
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { introPickerLauncher.launch("video/*") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NinjaDarkSurfaceVariant)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = NinjaRedGlow)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (settings.introVideoPath == null) "SELECT INTRO" else "CHANGE", color = NinjaRedGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (settings.introVideoPath != null) {
                                OutlinedButton(
                                    onClick = {
                                        previewVideoTitle = "Intro Video Preview"
                                        previewVideoPath = settings.introVideoPath
                                    },
                                    modifier = Modifier.weight(1f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NinjaRedPrimary)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = NinjaTextWhite)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PREVIEW", color = NinjaTextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(
                                    onClick = { viewModel.updateSettings(settings.copy(introVideoPath = null)) }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Intro", tint = Color.Red.copy(alpha = 0.8f))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Divider(color = NinjaCardOutline.copy(alpha = 0.5f))

                        // --- OUTRO VIDEO ---
                        Text("Outro Video", style = MaterialTheme.typography.labelLarge, color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Outro", color = NinjaTextMuted, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = settings.isOutroEnabled,
                                onCheckedChange = { viewModel.updateSettings(settings.copy(isOutroEnabled = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NinjaRedPrimary, checkedTrackColor = NinjaRedGlow.copy(alpha = 0.3f))
                            )
                        }

                        Text(
                            text = if (settings.outroVideoPath != null) "Selected: ${File(settings.outroVideoPath!!).name}" else "No outro video selected",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (settings.outroVideoPath != null) NinjaRedGlow else NinjaTextMuted
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { outroPickerLauncher.launch("video/*") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NinjaDarkSurfaceVariant)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = NinjaRedGlow)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (settings.outroVideoPath == null) "SELECT OUTRO" else "CHANGE", color = NinjaRedGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (settings.outroVideoPath != null) {
                                OutlinedButton(
                                    onClick = {
                                        previewVideoTitle = "Outro Video Preview"
                                        previewVideoPath = settings.outroVideoPath
                                    },
                                    modifier = Modifier.weight(1f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NinjaRedPrimary)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = NinjaTextWhite)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PREVIEW", color = NinjaTextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(
                                    onClick = { viewModel.updateSettings(settings.copy(outroVideoPath = null)) }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Outro", tint = Color.Red.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }

            // 2. BRANDING SECTION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("branding_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NinjaDarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NinjaCardOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BrandingWatermark, contentDescription = null, tint = NinjaRedPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "2. BRANDING",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    letterSpacing = 0.5.sp
                                ),
                                color = NinjaRedPrimary
                            )
                        }

                        Divider(color = NinjaCardOutline)

                        // Channel Name & Logo
                        OutlinedTextField(
                            value = channelNameState,
                            onValueChange = {
                                channelNameState = it
                                viewModel.updateSettings(settings.copy(channelName = it))
                            },
                            label = { Text("Channel Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NinjaRedPrimary,
                                unfocusedBorderColor = NinjaCardOutline,
                                focusedTextColor = NinjaTextWhite,
                                unfocusedTextColor = NinjaTextWhite
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Channel Logo", color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (settings.channelLogoPath != null) "Logo selected" else "No logo selected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NinjaTextMuted
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { logoPickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = NinjaDarkSurfaceVariant)
                                ) {
                                    Text(if (settings.channelLogoPath == null) "SELECT LOGO" else "CHANGE", color = NinjaRedGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                if (settings.channelLogoPath != null) {
                                    IconButton(onClick = { viewModel.updateSettings(settings.copy(channelLogoPath = null)) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove Logo", tint = Color.Red.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }

                        Divider(color = NinjaCardOutline.copy(alpha = 0.5f))

                        // Watermark Settings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Watermark", color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = settings.isWatermarkEnabled,
                                onCheckedChange = { viewModel.updateSettings(settings.copy(isWatermarkEnabled = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NinjaRedPrimary, checkedTrackColor = NinjaRedGlow.copy(alpha = 0.3f))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Watermark Image", color = NinjaTextWhite, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = if (settings.watermarkImagePath != null) "Watermark PNG selected" else "No watermark image",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NinjaTextMuted
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { watermarkPickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = NinjaDarkSurfaceVariant)
                                ) {
                                    Text(if (settings.watermarkImagePath == null) "SELECT WATERMARK" else "CHANGE", color = NinjaRedGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                if (settings.watermarkImagePath != null) {
                                    IconButton(onClick = { viewModel.updateSettings(settings.copy(watermarkImagePath = null)) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove Watermark", tint = Color.Red.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }

                        // Watermark Position
                        Text("Watermark Position", color = NinjaTextWhite, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            WatermarkPosition.values().forEach { pos ->
                                val posLabel = when(pos) {
                                    WatermarkPosition.TOP_LEFT -> "Top Left"
                                    WatermarkPosition.TOP_RIGHT -> "Top Right"
                                    WatermarkPosition.BOTTOM_LEFT -> "Bottom Left"
                                    WatermarkPosition.BOTTOM_RIGHT -> "Bottom Right"
                                }
                                val isSelected = settings.watermarkPosition == pos
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateSettings(settings.copy(watermarkPosition = pos)) },
                                    label = { Text(posLabel, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NinjaRedPrimary,
                                        selectedLabelColor = NinjaTextWhite,
                                        containerColor = NinjaDarkSurfaceVariant,
                                        labelColor = NinjaTextMuted
                                    )
                                )
                            }
                        }

                        // Watermark Opacity
                        Text("Watermark Opacity (${watermarkOpacityState.toInt()}%)", color = NinjaTextWhite, style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = watermarkOpacityState,
                            onValueChange = {
                                watermarkOpacityState = it
                                viewModel.updateSettings(settings.copy(watermarkOpacityPercent = it.toInt()))
                            },
                            valueRange = 10f..100f,
                            colors = SliderDefaults.colors(thumbColor = NinjaRedPrimary, activeTrackColor = NinjaRedPrimary)
                        )
                    }
                }
            }

            // 3. AUDIO SECTION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("audio_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NinjaDarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NinjaCardOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = NinjaRedPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "3. AUDIO",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    letterSpacing = 0.5.sp
                                ),
                                color = NinjaRedPrimary
                            )
                        }

                        Divider(color = NinjaCardOutline)

                        // Background Music
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Background Music", color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = settings.isBgMusicEnabled,
                                onCheckedChange = { viewModel.updateSettings(settings.copy(isBgMusicEnabled = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NinjaRedPrimary, checkedTrackColor = NinjaRedGlow.copy(alpha = 0.3f))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Background Track", color = NinjaTextWhite, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = if (settings.bgMusicPath != null) File(settings.bgMusicPath!!).name else "No custom track loaded",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NinjaTextMuted
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { bgMusicPickerLauncher.launch("audio/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = NinjaDarkSurfaceVariant)
                                ) {
                                    Text(if (settings.bgMusicPath == null) "SELECT MUSIC" else "CHANGE", color = NinjaRedGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                if (settings.bgMusicPath != null) {
                                    IconButton(onClick = { viewModel.updateSettings(settings.copy(bgMusicPath = null)) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove Music", tint = Color.Red.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }

                        Text("BG Music Volume (${(bgMusicVolState * 100).toInt()}%)", color = NinjaTextWhite, style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = bgMusicVolState,
                            onValueChange = {
                                bgMusicVolState = it
                                viewModel.updateSettings(settings.copy(bgMusicVolume = it))
                            },
                            valueRange = 0f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = NinjaRedPrimary, activeTrackColor = NinjaRedPrimary)
                        )

                        Divider(color = NinjaCardOutline.copy(alpha = 0.5f))

                        // Meme Sounds Volume
                        Text("Meme Sounds Volume (${(memeVolState * 100).toInt()}%)", color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                        Slider(
                            value = memeVolState,
                            onValueChange = {
                                memeVolState = it
                                viewModel.updateSettings(settings.copy(memeAudioVolume = it))
                            },
                            valueRange = 0f..1.5f,
                            colors = SliderDefaults.colors(thumbColor = NinjaRedPrimary, activeTrackColor = NinjaRedPrimary)
                        )

                        // Gameplay Volume
                        Text("Game Audio Volume (${(gameVolState * 100).toInt()}%)", color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                        Slider(
                            value = gameVolState,
                            onValueChange = {
                                gameVolState = it
                                viewModel.updateSettings(settings.copy(gameAudioVolume = it))
                            },
                            valueRange = 0f..1.5f,
                            colors = SliderDefaults.colors(thumbColor = NinjaRedPrimary, activeTrackColor = NinjaRedPrimary)
                        )
                    }
                }
            }

            // 4. MEMES SECTION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("memes_settings_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NinjaDarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NinjaCardOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEmotions, contentDescription = null, tint = NinjaRedPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "4. MEMES",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    letterSpacing = 0.5.sp
                                ),
                                color = NinjaRedPrimary
                            )
                        }

                        Divider(color = NinjaCardOutline)

                        // Meme Pack Selector
                        Text("Meme Pack", color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                        val memePacks = listOf("Default Gaming Pack", "MLG Soundboard Pack", "Twitch Memes Pack", "Anime FX Pack")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            memePacks.forEach { pack ->
                                val isSelected = settings.memePack == pack
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateSettings(settings.copy(memePack = pack)) },
                                    label = { Text(pack.replace(" Pack", ""), fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NinjaRedPrimary,
                                        selectedLabelColor = NinjaTextWhite,
                                        containerColor = NinjaDarkSurfaceVariant,
                                        labelColor = NinjaTextMuted
                                    )
                                )
                            }
                        }

                        // Meme Frequency
                        Text("Meme Frequency", color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MemeFrequency.values().forEach { freq ->
                                val label = when(freq) {
                                    MemeFrequency.LOW -> "LOW (20%)"
                                    MemeFrequency.MEDIUM -> "MEDIUM (50%)"
                                    MemeFrequency.HIGH -> "HIGH (80%)"
                                }
                                val isSelected = settings.memeFrequency == freq
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateMemeFrequency(freq) },
                                    label = { Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NinjaRedPrimary,
                                        selectedLabelColor = NinjaTextWhite,
                                        containerColor = NinjaDarkSurfaceVariant,
                                        labelColor = NinjaTextMuted
                                    )
                                )
                            }
                        }

                        // Auto Detection Toggles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Auto Meme Detection", color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                                Text("Insert memes automatically during killstreaks", style = MaterialTheme.typography.labelSmall, color = NinjaTextMuted)
                            }
                            Switch(
                                checked = settings.autoMemeDetection,
                                onCheckedChange = { viewModel.updateSettings(settings.copy(autoMemeDetection = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NinjaRedPrimary, checkedTrackColor = NinjaRedGlow.copy(alpha = 0.3f))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Master Memes Enabled", color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                                Text("Global meme overlay system toggle", style = MaterialTheme.typography.labelSmall, color = NinjaTextMuted)
                            }
                            Switch(
                                checked = settings.masterMemesEnabled,
                                onCheckedChange = { viewModel.toggleMasterMemes(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NinjaRedPrimary, checkedTrackColor = NinjaRedGlow.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }

            // 5. CAPTIONS SECTION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("captions_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NinjaDarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NinjaCardOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Subtitles, contentDescription = null, tint = NinjaRedPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "5. CAPTIONS",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    letterSpacing = 0.5.sp
                                ),
                                color = NinjaRedPrimary
                            )
                        }

                        Divider(color = NinjaCardOutline)

                        // Auto Captions Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Auto Captions", color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                                Text("Generate esports animated subtitles", style = MaterialTheme.typography.labelSmall, color = NinjaTextMuted)
                            }
                            Switch(
                                checked = settings.isCaptionsEnabled,
                                onCheckedChange = { viewModel.updateSettings(settings.copy(isCaptionsEnabled = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NinjaRedPrimary, checkedTrackColor = NinjaRedGlow.copy(alpha = 0.3f))
                            )
                        }

                        // Caption Font Choice
                        Text("Caption Font", color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                        val fonts = listOf("Roboto Bold", "Montserrat Black", "Bebas Neue", "Impact")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            fonts.forEach { fontName ->
                                val isSelected = settings.captionFont == fontName
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateSettings(settings.copy(captionFont = fontName)) },
                                    label = { Text(fontName.split(" ")[0], fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NinjaRedPrimary,
                                        selectedLabelColor = NinjaTextWhite,
                                        containerColor = NinjaDarkSurfaceVariant,
                                        labelColor = NinjaTextMuted
                                    )
                                )
                            }
                        }

                        // Caption Size
                        Text("Caption Size (${captionSizeState.toInt()} sp)", color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                        Slider(
                            value = captionSizeState,
                            onValueChange = {
                                captionSizeState = it
                                viewModel.updateSettings(settings.copy(captionSizeSp = it.toInt()))
                            },
                            valueRange = 16f..36f,
                            colors = SliderDefaults.colors(thumbColor = NinjaRedPrimary, activeTrackColor = NinjaRedPrimary)
                        )

                        // Caption Color
                        Text("Caption Text Color", color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                        val colorsList = listOf(
                            Pair("Yellow", "#FFFF00"),
                            Pair("White", "#FFFFFF"),
                            Pair("Cyan", "#00FFFF"),
                            Pair("Lime", "#00FF00"),
                            Pair("Red", "#FF0000")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colorsList.forEach { (colorName, hexCode) ->
                                val isSelected = settings.captionColorHex == hexCode
                                val chipColor = when(colorName) {
                                    "Yellow" -> Color.Yellow
                                    "White" -> Color.White
                                    "Cyan" -> Color.Cyan
                                    "Lime" -> Color.Green
                                    else -> Color.Red
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(chipColor.copy(alpha = 0.2f))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) NinjaRedPrimary else Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.updateSettings(settings.copy(captionColorHex = hexCode)) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(colorName, fontSize = 10.sp, color = chipColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 6. EXPORT SECTION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("export_settings_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NinjaDarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NinjaCardOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.IosShare, contentDescription = null, tint = NinjaRedPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "6. EXPORT",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    letterSpacing = 0.5.sp
                                ),
                                color = NinjaRedPrimary
                            )
                        }

                        Divider(color = NinjaCardOutline)

                        // Shorts Quality
                        Text("Shorts Quality", color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ExportQuality.values().forEach { q ->
                                val qLabel = when(q) {
                                    ExportQuality.P720 -> "720p HD"
                                    ExportQuality.P1080 -> "1080p FHD"
                                    ExportQuality.P4K -> "4K UHD"
                                }
                                val isSelected = settings.shortsQuality == q
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateSettings(settings.copy(shortsQuality = q)) },
                                    label = { Text(qLabel, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NinjaRedPrimary,
                                        selectedLabelColor = NinjaTextWhite,
                                        containerColor = NinjaDarkSurfaceVariant,
                                        labelColor = NinjaTextMuted
                                    )
                                )
                            }
                        }

                        // Long Video Quality
                        Text("Long Video Quality", color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ExportQuality.values().forEach { q ->
                                val qLabel = when(q) {
                                    ExportQuality.P720 -> "720p HD"
                                    ExportQuality.P1080 -> "1080p FHD"
                                    ExportQuality.P4K -> "4K UHD"
                                }
                                val isSelected = settings.longVideoQuality == q
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateSettings(settings.copy(longVideoQuality = q)) },
                                    label = { Text(qLabel, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NinjaRedPrimary,
                                        selectedLabelColor = NinjaTextWhite,
                                        containerColor = NinjaDarkSurfaceVariant,
                                        labelColor = NinjaTextMuted
                                    )
                                )
                            }
                        }

                        // FPS Selector
                        Text("Target Frame Rate (FPS)", color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(30, 60).forEach { fps ->
                                val isSelected = settings.exportFps == fps
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateSettings(settings.copy(exportFps = fps)) },
                                    label = { Text("$fps FPS", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NinjaRedPrimary,
                                        selectedLabelColor = NinjaTextWhite,
                                        containerColor = NinjaDarkSurfaceVariant,
                                        labelColor = NinjaTextMuted
                                    )
                                )
                            }
                        }

                        // Bitrate Selector
                        Text("Encoding Bitrate", color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(8, 12, 20, 30).forEach { bitrate ->
                                val isSelected = settings.exportBitrateMbps == bitrate
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateSettings(settings.copy(exportBitrateMbps = bitrate)) },
                                    label = { Text("$bitrate Mbps", fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NinjaRedPrimary,
                                        selectedLabelColor = NinjaTextWhite,
                                        containerColor = NinjaDarkSurfaceVariant,
                                        labelColor = NinjaTextMuted
                                    )
                                )
                            }
                        }

                        // Output Folder
                        OutlinedTextField(
                            value = outputFolderState,
                            onValueChange = {
                                outputFolderState = it
                                viewModel.updateSettings(settings.copy(outputFolder = it))
                            },
                            label = { Text("Output Folder Path") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NinjaRedPrimary,
                                unfocusedBorderColor = NinjaCardOutline,
                                focusedTextColor = NinjaTextWhite,
                                unfocusedTextColor = NinjaTextWhite
                            )
                        )
                    }
                }
            }

            // ABOUT THIS EDITOR CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("about_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NinjaDarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NinjaRedPrimary),
                    onClick = onNavigateToAbout
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "About",
                                tint = NinjaRedGlow,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "ABOUT THIS EDITOR",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NinjaTextWhite
                                )
                                Text(
                                    text = "Offline heuristic engine & signal processing details",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NinjaTextMuted
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open",
                            tint = NinjaTextMuted
                        )
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPreviewDialog(
    filePath: String,
    title: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember(filePath) {
        ExoPlayer.Builder(context).build().apply {
            val uri = if (filePath.startsWith("content://") || filePath.startsWith("file://")) {
                Uri.parse(filePath)
            } else {
                Uri.fromFile(File(filePath))
            }
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(filePath) {
        onDispose {
            exoPlayer.release()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NinjaTextWhite) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NinjaRedPrimary)
            ) {
                Text("CLOSE", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = NinjaDarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
