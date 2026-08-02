package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM editor_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<EditorSettings?>

    @Query("SELECT * FROM editor_settings WHERE id = 1")
    suspend fun getSettings(): EditorSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: EditorSettings)
}

@Dao
interface MemeDao {
    @Query("SELECT * FROM meme_assets ORDER BY dateAddedMs DESC")
    fun getAllMemesFlow(): Flow<List<MemeAsset>>

    @Query("SELECT * FROM meme_assets WHERE category = :category ORDER BY dateAddedMs DESC")
    fun getMemesByCategoryFlow(category: MemeCategory): Flow<List<MemeAsset>>

    @Query("SELECT * FROM meme_assets WHERE category = :category")
    suspend fun getMemesByCategory(category: MemeCategory): List<MemeAsset>

    @Query("SELECT * FROM meme_assets")
    suspend fun getAllMemes(): List<MemeAsset>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeme(meme: MemeAsset): Long

    @Delete
    suspend fun deleteMeme(meme: MemeAsset)
}

@Dao
interface ExportDao {
    @Query("SELECT * FROM export_projects ORDER BY createdTimestampMs DESC")
    fun getAllProjectsFlow(): Flow<List<ExportProject>>

    @Query("SELECT * FROM export_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ExportProject?

    @Query("SELECT * FROM export_projects WHERE status = 'PROCESSING' OR status = 'QUEUED' ORDER BY createdTimestampMs DESC LIMIT 1")
    fun getActiveProjectFlow(): Flow<ExportProject?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ExportProject): Long

    @Update
    suspend fun updateProject(project: ExportProject)

    @Query("UPDATE export_projects SET progressPercent = :progressPercent, status = :status, outputFilePath = :filePath, errorMessage = :errorMsg WHERE id = :id")
    suspend fun updateProgress(id: Long, progressPercent: Int, status: ExportStatus, filePath: String? = null, errorMsg: String? = null)

    @Delete
    suspend fun deleteProject(project: ExportProject)
}
