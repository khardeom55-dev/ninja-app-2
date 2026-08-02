package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MemeFrequency {
    LOW, MEDIUM, HIGH
}

enum class OutputMode {
    SHORTS, LONG, BOTH
}

enum class ExportQuality {
    P720, P1080, P4K
}

enum class WatermarkPosition {
    TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT
}

@Entity(tableName = "editor_settings")
data class EditorSettings(
    @PrimaryKey val id: Int = 1,
    val channelName: String = "King Ninja55",
    val channelLogoPath: String? = null,
    val introVideoPath: String? = null,
    val outroVideoPath: String? = null,
    val watermarkImagePath: String? = null,
    val preferredFontPath: String? = null,
    val bgMusicPath: String? = null,
    
    val memeFrequency: MemeFrequency = MemeFrequency.LOW,
    val masterMemesEnabled: Boolean = true,
    val autoMemeDetection: Boolean = true,
    val memePack: String = "Default Gaming Pack",
    val soundFrequency: String = "NORMAL",
    val editingIntensity: String = "BALANCED",
    
    val isCaptionsEnabled: Boolean = true,
    val captionFont: String = "Roboto Bold",
    val captionSizeSp: Int = 24,
    val captionColorHex: String = "#FFFF00",
    
    val isBgMusicEnabled: Boolean = false,
    
    val minShortDurationSec: Int = 15,
    val maxShortDurationSec: Int = 35,
    val numberOfShorts: Int = 5,
    
    val watermarkPosition: WatermarkPosition = WatermarkPosition.TOP_RIGHT,
    val watermarkOpacityPercent: Int = 80,
    val watermarkScalePercent: Int = 15,
    val isWatermarkEnabled: Boolean = true,
    
    val isIntroEnabled: Boolean = true,
    val isOutroEnabled: Boolean = true,
    
    val gameAudioVolume: Float = 1.0f,
    val memeAudioVolume: Float = 0.8f,
    val bgMusicVolume: Float = 0.4f,
    
    val shortsQuality: ExportQuality = ExportQuality.P1080,
    val longVideoQuality: ExportQuality = ExportQuality.P1080,
    val defaultOutputQuality: ExportQuality = ExportQuality.P1080,
    val exportFps: Int = 60,
    val exportBitrateMbps: Int = 12,
    val outputFolder: String = "Movies/NinjaAutoEditor"
)
