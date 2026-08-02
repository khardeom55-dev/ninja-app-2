package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MemeCategory {
    FUNNY, FAIL, HEADSHOT, VICTORY, SHOCK, RAGE
}

enum class MemeMediaType {
    IMAGE, AUDIO, VIDEO
}

@Entity(tableName = "meme_assets")
data class MemeAsset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val mediaType: MemeMediaType,
    val category: MemeCategory,
    val durationMs: Long = 0,
    val dateAddedMs: Long = System.currentTimeMillis()
)
