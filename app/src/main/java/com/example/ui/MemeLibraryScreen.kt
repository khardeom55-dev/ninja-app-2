package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MemeAsset
import com.example.data.MemeCategory
import com.example.data.MemeMediaType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemeLibraryScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    val memes by viewModel.memes.collectAsStateWithLifecycle()
    var selectedCategoryFilter by remember { mutableStateOf<MemeCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            pendingImportUri = uri
            showAddDialog = true
        }
    }

    val filteredMemes = if (selectedCategoryFilter == null) {
        memes
    } else {
        memes.filter { it.category == selectedCategoryFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LOCAL MEME LIBRARY", color = NinjaTextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NinjaTextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NinjaDarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch("*/*") },
                containerColor = NinjaRedPrimary,
                contentColor = NinjaTextWhite,
                modifier = Modifier.testTag("add_meme_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import Meme")
            }
        },
        containerColor = NinjaDarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Category Filter Scrollable Row
            ScrollableTabRow(
                selectedTabIndex = if (selectedCategoryFilter == null) 0 else selectedCategoryFilter!!.ordinal + 1,
                containerColor = NinjaDarkBackground,
                contentColor = NinjaRedGlow,
                edgePadding = 0.dp
            ) {
                Tab(
                    selected = selectedCategoryFilter == null,
                    onClick = { selectedCategoryFilter = null },
                    text = { Text("ALL", fontWeight = FontWeight.Bold) }
                )
                MemeCategory.entries.forEach { category ->
                    Tab(
                        selected = selectedCategoryFilter == category,
                        onClick = { selectedCategoryFilter = category },
                        text = { Text(category.name, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredMemes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SentimentVerySatisfied,
                            contentDescription = null,
                            tint = NinjaRedPrimary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "NO MEMES IN THIS CATEGORY",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NinjaTextWhite
                        )
                        Text(
                            text = "Tap the + button to import custom PNG, JPG, GIF, MP3, or MP4 meme files from your phone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NinjaTextMuted,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredMemes, key = { it.id }) { meme ->
                        MemeItemCard(
                            meme = meme,
                            onDelete = { viewModel.deleteMemeAsset(meme) }
                        )
                    }
                }
            }
        }
    }

    // Category Selector Dialog on File Import
    if (showAddDialog && pendingImportUri != null) {
        var memeTitle by remember { mutableStateOf("Custom Meme") }
        var chosenCategory by remember { mutableStateOf(MemeCategory.FUNNY) }
        var chosenType by remember { mutableStateOf(MemeMediaType.IMAGE) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                pendingImportUri = null
            },
            title = { Text("Import Meme Asset", color = NinjaTextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = memeTitle,
                        onValueChange = { memeTitle = it },
                        label = { Text("Title / Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NinjaRedPrimary,
                            unfocusedBorderColor = NinjaCardOutline,
                            focusedTextColor = NinjaTextWhite,
                            unfocusedTextColor = NinjaTextWhite
                        )
                    )

                    Text("Select Category:", color = NinjaTextWhite, style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MemeCategory.entries.take(3).forEach { cat ->
                            FilterChip(
                                selected = chosenCategory == cat,
                                onClick = { chosenCategory = cat },
                                label = { Text(cat.name, fontSize = 10.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MemeCategory.entries.drop(3).forEach { cat ->
                            FilterChip(
                                selected = chosenCategory == cat,
                                onClick = { chosenCategory = cat },
                                label = { Text(cat.name, fontSize = 10.sp) }
                            )
                        }
                    }

                    Text("Media Type:", color = NinjaTextWhite, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MemeMediaType.entries.forEach { type ->
                            FilterChip(
                                selected = chosenType == type,
                                onClick = { chosenType = type },
                                label = { Text(type.name) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addMemeAsset(
                            title = memeTitle,
                            filePath = pendingImportUri.toString(),
                            mediaType = chosenType,
                            category = chosenCategory
                        )
                        showAddDialog = false
                        pendingImportUri = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NinjaRedPrimary)
                ) {
                    Text("SAVE MEME")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        pendingImportUri = null
                    }
                ) {
                    Text("CANCEL", color = NinjaTextMuted)
                }
            },
            containerColor = NinjaDarkSurface
        )
    }
}

@Composable
fun MemeItemCard(
    meme: MemeAsset,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NinjaDarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NinjaCardOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(NinjaDarkSurfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (meme.mediaType) {
                            MemeMediaType.IMAGE -> Icons.Default.Image
                            MemeMediaType.AUDIO -> Icons.Default.Audiotrack
                            MemeMediaType.VIDEO -> Icons.Default.Videocam
                        },
                        contentDescription = null,
                        tint = NinjaRedGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(meme.title, color = NinjaTextWhite, fontWeight = FontWeight.Bold)
                    Text("${meme.category.name} | ${meme.mediaType.name}", style = MaterialTheme.typography.labelSmall, color = NinjaTextMuted)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NinjaRedGlow)
            }
        }
    }
}
