package com.example.editor

import kotlin.math.max
import kotlin.math.min

class HighlightDetector {

    private val hookTemplates = listOf(
        "INSANE CLUTCH 🔥",
        "HEADSHOT MASTER 🎯",
        "UNSTOPPABLE RUN ⚡",
        "EPIC NINJA PLAY 👑",
        "GOD MODE ACTIVATED 💥",
        "IMPRACTICAL PLAY 😱",
        "1v4 CLUTCH MOMENT 🏆",
        "FULL SEND ENERGY 🚀",
        "PUNISHED THEM ALL ⚔️",
        "KING NINJA55 MOMENT 🎮"
    )

    private val subtitleTemplates = listOf(
        "GET RECKT!",
        "NO WAY BRO!",
        "CLEAN HEADSHOT!",
        "TRIPLE KILL!",
        "ABSOLUTE MADNESS!",
        "TOO EASY!",
        "VICTORY SECURED!"
    )

    /**
     * Finds 3 to 10 non-overlapping short clips (15s to 35s each).
     */
    fun findHighlightShorts(
        peaks: List<AudioPeak>,
        videoDurationMs: Long,
        minDurationSec: Int = 15,
        maxDurationSec: Int = 35,
        targetClipCount: Int = 5
    ): List<HighlightSegment> {
        if (videoDurationMs < minDurationSec * 1000L) {
            // Video is shorter than min duration, return whole video as single clip
            return listOf(
                HighlightSegment(
                    id = 1,
                    startTimeMs = 0L,
                    endTimeMs = videoDurationMs,
                    score = 1.0f,
                    hookText = hookTemplates.first(),
                    subtitleText = subtitleTemplates.first()
                )
            )
        }

        val targetDurationMs = ((minDurationSec + maxDurationSec) / 2) * 1000L
        val candidateWindows = mutableListOf<HighlightSegment>()

        val windowStepMs = 2000L // slide window every 2 seconds
        var startMs = 0L

        val padStartMs = 3000L
        val padEndMs = videoDurationMs - 3000L

        while (startMs + minDurationSec * 1000L <= videoDurationMs) {
            val endMs = min(startMs + targetDurationMs, videoDurationMs)
            
            // Exclude start/end boundary windows unless peak energy is extremely high (> 0.85)
            val isBoundaryWindow = startMs < padStartMs || endMs > padEndMs
            val windowPeaks = peaks.filter { it.timestampMs in startMs..endMs }

            if (windowPeaks.isNotEmpty()) {
                val avgEnergy = windowPeaks.map { it.amplitude }.average().toFloat()
                val burstCount = windowPeaks.count { it.isGunshotOrBurst }
                val maxEnergy = windowPeaks.maxOfOrNull { it.amplitude } ?: 0f

                val isStrongEnough = if (isBoundaryWindow) maxEnergy > 0.85f else (avgEnergy > 0.15f || maxEnergy > 0.5f)

                if (isStrongEnough) {
                    val score = (avgEnergy * 0.5f) + (burstCount * 0.15f) + (maxEnergy * 0.35f)
                    val hookIndex = candidateWindows.size % hookTemplates.size
                    val subIndex = candidateWindows.size % subtitleTemplates.size
                    candidateWindows.add(
                        HighlightSegment(
                            id = candidateWindows.size + 1,
                            startTimeMs = startMs,
                            endTimeMs = endMs,
                            score = score,
                            hookText = hookTemplates[hookIndex],
                            subtitleText = subtitleTemplates[subIndex]
                        )
                    )
                }
            }

            startMs += windowStepMs
        }

        // Sort by highest energy score
        candidateWindows.sortByDescending { it.score }

        // Select top non-overlapping clips
        val selectedClips = mutableListOf<HighlightSegment>()
        for (candidate in candidateWindows) {
            if (selectedClips.size >= targetClipCount) break

            // Check overlap with existing selected clips (minimum 5 sec spacing)
            val overlaps = selectedClips.any { existing ->
                max(candidate.startTimeMs, existing.startTimeMs) < min(candidate.endTimeMs, existing.endTimeMs) + 5000L
            }

            if (!overlaps) {
                selectedClips.add(candidate.copy(id = selectedClips.size + 1))
            }
        }

        // Sort selected clips chronologically by start time
        selectedClips.sortBy { it.startTimeMs }

        // If no high energy segments found, fallback to even chunk slicing
        if (selectedClips.isEmpty()) {
            val chunkMs = targetDurationMs
            var currentMs = 0L
            var idCounter = 1
            while (currentMs + minDurationSec * 1000L <= videoDurationMs && selectedClips.size < targetClipCount) {
                val clipEnd = min(currentMs + chunkMs, videoDurationMs)
                selectedClips.add(
                    HighlightSegment(
                        id = idCounter,
                        startTimeMs = currentMs,
                        endTimeMs = clipEnd,
                        score = 0.5f,
                        hookText = hookTemplates[(idCounter - 1) % hookTemplates.size],
                        subtitleText = subtitleTemplates[(idCounter - 1) % subtitleTemplates.size]
                    )
                )
                currentMs += chunkMs + 3000L
                idCounter++
            }
        }

        return selectedClips
    }

    /**
     * Filters out silent/flat gaps (>3 seconds low audio) for Auto Long Video editing.
     */
    fun findActiveLongVideoRanges(
        peaks: List<AudioPeak>,
        videoDurationMs: Long,
        silenceThreshold: Float = 0.12f
    ): List<Pair<Long, Long>> {
        val activeRanges = mutableListOf<Pair<Long, Long>>()
        if (peaks.isEmpty()) {
            return listOf(0L to videoDurationMs)
        }

        var inActiveRange = false
        var rangeStart = 0L
        var silentStartTime = 0L

        for (peak in peaks) {
            val isSilent = peak.amplitude < silenceThreshold

            if (!inActiveRange) {
                if (!isSilent) {
                    inActiveRange = true
                    rangeStart = max(0L, peak.timestampMs - 1000L) // include 1s lead-in
                }
            } else {
                if (isSilent) {
                    if (silentStartTime == 0L) {
                        silentStartTime = peak.timestampMs
                    } else if (peak.timestampMs - silentStartTime > 3000L) {
                        // Silent for more than 3 seconds, close range
                        inActiveRange = false
                        activeRanges.add(rangeStart to silentStartTime)
                        silentStartTime = 0L
                    }
                } else {
                    silentStartTime = 0L
                }
            }
        }

        if (inActiveRange) {
            activeRanges.add(rangeStart to videoDurationMs)
        }

        // If cut ranges became empty or too aggressive, return original full video
        if (activeRanges.isEmpty()) {
            return listOf(0L to videoDurationMs)
        }

        return activeRanges
    }
}
