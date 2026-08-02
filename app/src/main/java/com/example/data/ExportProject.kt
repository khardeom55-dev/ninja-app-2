package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ExportStatus {
    QUEUED, PROCESSING, COMPLETED, FAILED, CANCELLED
}

@Entity(tableName = "export_projects")
data class ExportProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceVideoUri: String,
    val outputType: OutputMode,
    val quality: ExportQuality,
    val outputFilePath: String? = null,
    val thumbnailPath: String? = null,
    val progressPercent: Int = 0,
    val status: ExportStatus = ExportStatus.QUEUED,
    val errorMessage: String? = null,
    val durationMs: Long = 0,
    val createdTimestampMs: Long = System.currentTimeMillis()
)
