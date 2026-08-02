package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.*
import com.example.editor.VideoAnalyzer
import com.example.editor.VideoMetadata
import com.example.worker.VideoExportWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val workManager by lazy {
        try {
            WorkManager.getInstance(application)
        } catch (e: IllegalStateException) {
            androidx.work.WorkManager.initialize(
                application,
                androidx.work.Configuration.Builder().build()
            )
            WorkManager.getInstance(application)
        }
    }
    private val analyzer = VideoAnalyzer(application)

    val settings: StateFlow<EditorSettings> = db.settingsDao().getSettingsFlow()
        .map { it ?: EditorSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EditorSettings())

    val memes: StateFlow<List<MemeAsset>> = db.memeDao().getAllMemesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<ExportProject>> = db.exportDao().getAllProjectsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProject: StateFlow<ExportProject?> = db.exportDao().getActiveProjectFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedVideoUri = MutableStateFlow<Uri?>(null)
    val selectedVideoUri: StateFlow<Uri?> = _selectedVideoUri.asStateFlow()

    private val _selectedVideoMetadata = MutableStateFlow<VideoMetadata?>(null)
    val selectedVideoMetadata: StateFlow<VideoMetadata?> = _selectedVideoMetadata.asStateFlow()

    private val _selectedOutputMode = MutableStateFlow(OutputMode.SHORTS)
    val selectedOutputMode: StateFlow<OutputMode> = _selectedOutputMode.asStateFlow()

    private val _selectedQuality = MutableStateFlow(ExportQuality.P1080)
    val selectedQuality: StateFlow<ExportQuality> = _selectedQuality.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    fun selectVideo(uri: Uri) {
        _isAnalyzing.value = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val sourceDir = java.io.File(app.filesDir, "source_videos")
                if (!sourceDir.exists()) sourceDir.mkdirs()

                var fileName = "gameplay_${System.currentTimeMillis()}.mp4"
                if (uri.scheme == "content") {
                    app.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                } else if (uri.scheme == "file") {
                    uri.lastPathSegment?.let { fileName = it }
                }

                val localFile = java.io.File(sourceDir, fileName)

                if (uri.scheme == "content") {
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        localFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } else if (uri.scheme == "file") {
                    val srcFile = java.io.File(uri.path ?: "")
                    if (srcFile.exists() && srcFile.absolutePath != localFile.absolutePath) {
                        srcFile.copyTo(localFile, overwrite = true)
                    }
                }

                val targetUri = if (localFile.exists() && localFile.length() > 0) Uri.fromFile(localFile) else uri
                _selectedVideoUri.value = targetUri

                val metadata = analyzer.extractMetadata(targetUri)
                _selectedVideoMetadata.value = metadata
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "Error copying video", e)
                _selectedVideoUri.value = uri
                _selectedVideoMetadata.value = analyzer.extractMetadata(uri)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun setOutputMode(mode: OutputMode) {
        _selectedOutputMode.value = mode
    }

    fun setQuality(quality: ExportQuality) {
        _selectedQuality.value = quality
    }

    fun updateMemeFrequency(frequency: MemeFrequency) {
        viewModelScope.launch {
            val current = settings.value
            db.settingsDao().insertOrUpdateSettings(current.copy(memeFrequency = frequency))
        }
    }

    fun toggleMasterMemes(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            db.settingsDao().insertOrUpdateSettings(current.copy(masterMemesEnabled = enabled))
        }
    }

    fun updateSettings(newSettings: EditorSettings) {
        viewModelScope.launch {
            db.settingsDao().insertOrUpdateSettings(newSettings)
        }
    }

    fun startAutoEdit() {
        val uri = selectedVideoUri.value ?: return
        val metadata = selectedVideoMetadata.value ?: return

        viewModelScope.launch {
            val project = ExportProject(
                title = metadata.fileName.replace(".mp4", "", ignoreCase = true),
                sourceVideoUri = uri.toString(),
                outputType = selectedOutputMode.value,
                quality = selectedQuality.value,
                durationMs = metadata.durationMs,
                status = ExportStatus.QUEUED
            )

            val projectId = db.exportDao().insertProject(project)

            val inputData = Data.Builder()
                .putLong(VideoExportWorker.KEY_PROJECT_ID, projectId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<VideoExportWorker>()
                .setInputData(inputData)
                .build()

            workManager.enqueue(workRequest)
        }
    }

    fun addMemeAsset(title: String, filePath: String, mediaType: MemeMediaType, category: MemeCategory) {
        viewModelScope.launch {
            val meme = MemeAsset(
                title = title,
                filePath = filePath,
                mediaType = mediaType,
                category = category
            )
            db.memeDao().insertMeme(meme)
        }
    }

    fun deleteMemeAsset(meme: MemeAsset) {
        viewModelScope.launch {
            db.memeDao().deleteMeme(meme)
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            db.settingsDao().insertOrUpdateSettings(EditorSettings())
        }
    }

    fun importCustomAsset(uri: Uri, type: String, title: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val targetDir = java.io.File(app.filesDir, type)
                if (!targetDir.exists()) targetDir.mkdirs()

                val ext = if (type == "logos" || type == "watermarks") ".png" else ".mp4"
                val fileName = "${type}_${System.currentTimeMillis()}$ext"
                val destFile = java.io.File(targetDir, fileName)

                if (uri.scheme == "content") {
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } else if (uri.scheme == "file") {
                    val srcFile = java.io.File(uri.path ?: "")
                    if (srcFile.exists()) srcFile.copyTo(destFile, overwrite = true)
                }

                if (destFile.exists() && destFile.length() > 0) {
                    val current = settings.value
                    when (type) {
                        "logos" -> updateSettings(current.copy(channelLogoPath = destFile.absolutePath))
                        "watermarks" -> updateSettings(current.copy(watermarkImagePath = destFile.absolutePath, isWatermarkEnabled = true))
                        "intros" -> updateSettings(current.copy(introVideoPath = destFile.absolutePath, isIntroEnabled = true))
                        "outros" -> updateSettings(current.copy(outroVideoPath = destFile.absolutePath, isOutroEnabled = true))
                        "music" -> updateSettings(current.copy(bgMusicPath = destFile.absolutePath, isBgMusicEnabled = true))
                        "memes" -> addMemeAsset(title, destFile.absolutePath, MemeMediaType.VIDEO, MemeCategory.FUNNY)
                        "sounds" -> addMemeAsset(title, destFile.absolutePath, MemeMediaType.AUDIO, MemeCategory.FUNNY)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "Failed to import asset $type", e)
            }
        }
    }

    fun deleteProject(project: ExportProject) {
        viewModelScope.launch {
            db.exportDao().deleteProject(project)
        }
    }
}
