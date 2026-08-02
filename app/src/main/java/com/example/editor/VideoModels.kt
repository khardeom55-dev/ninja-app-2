package com.example.editor

import android.net.Uri

data class VideoMetadata(
    val uri: Uri,
    val fileName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotation: Int = 0,
    val frameRate: Float = 30f,
    val sizeBytes: Long = 0,
    val hasAudio: Boolean = true
)

data class AudioPeak(
    val timestampMs: Long,
    val amplitude: Float, // 0.0 to 1.0 energy
    val isGunshotOrBurst: Boolean = false
)

data class HighlightSegment(
    val id: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val score: Float,
    val hookText: String,
    val subtitleText: String = "",
    val suggestedEffect: String = "ZOOM_PULSE"
) {
    val durationSec: Int get() = ((endTimeMs - startTimeMs) / 1000).toInt()
}
