package com.example.editor

import android.content.Context
import android.graphics.*
import android.media.*
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Effects
import androidx.media3.transformer.*
import com.example.data.EditorSettings
import com.example.data.ExportQuality
import com.example.data.MemeAsset
import com.example.data.OutputMode
import com.example.data.WatermarkPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
class VideoProcessingEngine(private val context: Context) {

    private val mediaStoreExporter = MediaStoreExporter(context)

    interface ProgressListener {
        fun onProgress(percent: Int)
    }

    suspend fun processAndExport(
        inputUri: Uri,
        outputMode: OutputMode,
        quality: ExportQuality,
        settings: EditorSettings,
        memes: List<MemeAsset>,
        listener: ProgressListener
    ): List<File> = withContext(Dispatchers.IO) {
        val exportedFiles = mutableListOf<File>()
        val analyzer = VideoAnalyzer(context)
        val detector = HighlightDetector()

        listener.onProgress(5)
        listener.onProgress(15)
        val metadata = analyzer.extractMetadata(inputUri)
        listener.onProgress(25)
        val peaks = analyzer.analyzeAudioPeaks(inputUri, metadata.durationMs)
        listener.onProgress(40)

        when (outputMode) {
            OutputMode.SHORTS -> {
                val shortsSegments = detector.findHighlightShorts(
                    peaks = peaks,
                    videoDurationMs = metadata.durationMs,
                    minDurationSec = settings.minShortDurationSec,
                    maxDurationSec = settings.maxShortDurationSec,
                    targetClipCount = settings.numberOfShorts
                )
                listener.onProgress(50)

                val stepProgress = 35f / shortsSegments.size.coerceAtLeast(1)
                shortsSegments.forEachIndexed { index, segment ->
                    val shortName = "KingNinja55_Short_${String.format("%03d", index + 1)}.mp4"
                    val tempOutFile = File(context.cacheDir, "temp_$shortName")

                    val success = exportShortClip(
                        inputUri = inputUri,
                        segment = segment,
                        quality = quality,
                        settings = settings,
                        destFile = tempOutFile
                    )

                    listener.onProgress((50 + (index + 0.5f) * stepProgress).toInt().coerceAtMost(85))

                    if (success && verifyExportOutput(tempOutFile)) {
                        listener.onProgress(92)
                        val savedFile = mediaStoreExporter.saveVideoToNinjaFolder(tempOutFile, shortName, "Shorts")
                        listener.onProgress(98)
                        exportedFiles.add(savedFile)
                        tempOutFile.delete()
                    }
                }
            }

            OutputMode.LONG -> {
                val longName = "KingNinja55_Long_Edit.mp4"
                val tempOutFile = File(context.cacheDir, "temp_$longName")

                val activeRanges = detector.findActiveLongVideoRanges(peaks, metadata.durationMs)
                val success = exportLongVideo(
                    inputUri = inputUri,
                    activeRanges = activeRanges,
                    quality = quality,
                    settings = settings,
                    memes = memes,
                    destFile = tempOutFile
                )

                if (success && verifyExportOutput(tempOutFile)) {
                    val savedFile = mediaStoreExporter.saveVideoToNinjaFolder(tempOutFile, longName, "LongVideos")
                    exportedFiles.add(savedFile)
                    tempOutFile.delete()
                }
                listener.onProgress(95)
            }

            OutputMode.BOTH -> {
                // Export Shorts first
                val shortsSegments = detector.findHighlightShorts(
                    peaks = peaks,
                    videoDurationMs = metadata.durationMs,
                    minDurationSec = settings.minShortDurationSec,
                    maxDurationSec = settings.maxShortDurationSec,
                    targetClipCount = settings.numberOfShorts
                )

                shortsSegments.forEachIndexed { index, segment ->
                    val shortName = "KingNinja55_Short_${String.format("%03d", index + 1)}.mp4"
                    val tempOutFile = File(context.cacheDir, "temp_$shortName")

                    val success = exportShortClip(
                        inputUri = inputUri,
                        segment = segment,
                        quality = quality,
                        settings = settings,
                        destFile = tempOutFile
                    )

                    if (success && verifyExportOutput(tempOutFile)) {
                        val savedFile = mediaStoreExporter.saveVideoToNinjaFolder(tempOutFile, shortName, "Shorts")
                        exportedFiles.add(savedFile)
                        tempOutFile.delete()
                    }
                }
                listener.onProgress(60)

                // Export Long Video second
                val longName = "KingNinja55_Long_Edit.mp4"
                val tempOutFile = File(context.cacheDir, "temp_$longName")
                val activeRanges = detector.findActiveLongVideoRanges(peaks, metadata.durationMs)
                val success = exportLongVideo(
                    inputUri = inputUri,
                    activeRanges = activeRanges,
                    quality = quality,
                    settings = settings,
                    memes = memes,
                    destFile = tempOutFile
                )

                if (success && verifyExportOutput(tempOutFile)) {
                    val savedFile = mediaStoreExporter.saveVideoToNinjaFolder(tempOutFile, longName, "LongVideos")
                    exportedFiles.add(savedFile)
                    tempOutFile.delete()
                }
                listener.onProgress(95)
            }
        }

        listener.onProgress(100)
        return@withContext exportedFiles
    }

    private fun exportShortClip(
        inputUri: Uri,
        segment: HighlightSegment,
        quality: ExportQuality,
        settings: EditorSettings,
        destFile: File
    ): Boolean {
        return try {
            val targetWidth = if (quality == ExportQuality.P1080) 1080 else 720
            val targetHeight = if (quality == ExportQuality.P1080) 1920 else 1280

            val effectsList = mutableListOf<Effect>()
            val presentation = Presentation.createForWidthAndHeight(targetWidth, targetHeight, Presentation.LAYOUT_SCALE_TO_FIT)
            effectsList.add(presentation)

            val watermarkPath = settings.watermarkImagePath ?: settings.channelLogoPath
            if (settings.isWatermarkEnabled && !watermarkPath.isNullOrEmpty() && File(watermarkPath).exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(watermarkPath)
                    if (bitmap != null) {
                        val overlay = BitmapOverlay.createStaticBitmapOverlay(bitmap) as androidx.media3.effect.TextureOverlay
                        effectsList.add(OverlayEffect(com.google.common.collect.ImmutableList.of(overlay)))
                    }
                } catch (e: Exception) {
                    Log.e("VideoProcessingEngine", "Failed to load watermark bitmap", e)
                }
            }

            val effects = Effects(emptyList(), effectsList)

            val mainMediaItem = MediaItem.Builder()
                .setUri(inputUri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(segment.startTimeMs)
                        .setEndPositionMs(segment.endTimeMs)
                        .build()
                )
                .build()

            val mainEditedItem = EditedMediaItem.Builder(mainMediaItem)
                .setEffects(effects)
                .build()

            val sequenceItems = mutableListOf<EditedMediaItem>()

            // Intro if enabled
            if (settings.isIntroEnabled && !settings.introVideoPath.isNullOrEmpty() && File(settings.introVideoPath).exists()) {
                val introItem = MediaItem.fromUri(Uri.fromFile(File(settings.introVideoPath)))
                val introEdited = EditedMediaItem.Builder(introItem).setEffects(effects).build()
                sequenceItems.add(introEdited)
            }

            sequenceItems.add(mainEditedItem)

            // Outro if enabled
            if (settings.isOutroEnabled && !settings.outroVideoPath.isNullOrEmpty() && File(settings.outroVideoPath).exists()) {
                val outroItem = MediaItem.fromUri(Uri.fromFile(File(settings.outroVideoPath)))
                val outroEdited = EditedMediaItem.Builder(outroItem).setEffects(effects).build()
                sequenceItems.add(outroEdited)
            }

            val sequence = EditedMediaItemSequence(sequenceItems)
            val composition = Composition.Builder(listOf(sequence)).build()

            var success = runTransformerCompositionExportSync(composition, destFile)

            if (!success || !verifyExportOutput(destFile)) {
                success = runDirectMediaMuxerClip(inputUri, segment.startTimeMs, segment.endTimeMs, destFile)
            }
            success && verifyExportOutput(destFile)
        } catch (e: Exception) {
            Log.e("VideoProcessingEngine", "Error exporting short clip", e)
            val fallbackSuccess = runDirectMediaMuxerClip(inputUri, segment.startTimeMs, segment.endTimeMs, destFile)
            fallbackSuccess && verifyExportOutput(destFile)
        }
    }

    private fun exportLongVideo(
        inputUri: Uri,
        activeRanges: List<Pair<Long, Long>>,
        quality: ExportQuality,
        settings: EditorSettings,
        memes: List<MemeAsset>,
        destFile: File
    ): Boolean {
        return try {
            val sequenceItems = mutableListOf<EditedMediaItem>()

            // Intro if enabled
            if (settings.isIntroEnabled && !settings.introVideoPath.isNullOrEmpty() && File(settings.introVideoPath).exists()) {
                val introItem = MediaItem.fromUri(Uri.fromFile(File(settings.introVideoPath)))
                sequenceItems.add(EditedMediaItem.Builder(introItem).build())
            }

            val ranges = if (activeRanges.isNotEmpty()) activeRanges else listOf(0L to minOf(60000L, settings.maxShortDurationSec * 1000L))
            ranges.forEach { range ->
                val mediaItem = MediaItem.Builder()
                    .setUri(inputUri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(range.first)
                            .setEndPositionMs(range.second)
                            .build()
                    )
                    .build()
                sequenceItems.add(EditedMediaItem.Builder(mediaItem).build())
            }

            // Outro if enabled
            if (settings.isOutroEnabled && !settings.outroVideoPath.isNullOrEmpty() && File(settings.outroVideoPath).exists()) {
                val outroItem = MediaItem.fromUri(Uri.fromFile(File(settings.outroVideoPath)))
                sequenceItems.add(EditedMediaItem.Builder(outroItem).build())
            }

            val sequence = EditedMediaItemSequence(sequenceItems)
            val composition = Composition.Builder(listOf(sequence)).build()

            var success = runTransformerCompositionExportSync(composition, destFile)

            if (!success || !verifyExportOutput(destFile)) {
                val duration = ranges.firstOrNull()?.second ?: 60000L
                success = runDirectMediaMuxerClip(inputUri, 0L, duration, destFile)
            }
            success && verifyExportOutput(destFile)
        } catch (e: Exception) {
            Log.e("VideoProcessingEngine", "Error exporting long video", e)
            val duration = activeRanges.firstOrNull()?.second ?: 60000L
            val fallbackSuccess = runDirectMediaMuxerClip(inputUri, 0L, duration, destFile)
            fallbackSuccess && verifyExportOutput(destFile)
        }
    }

    private fun runTransformerCompositionExportSync(composition: Composition, destFile: File): Boolean {
        var completed = false
        var failed = false
        val latch = CountDownLatch(1)

        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            try {
                val transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(comp: Composition, exportResult: ExportResult) {
                            completed = true
                            latch.countDown()
                        }

                        override fun onError(comp: Composition, exportResult: ExportResult, exportException: ExportException) {
                            Log.e("VideoProcessingEngine", "Transformer error", exportException)
                            failed = true
                            latch.countDown()
                        }
                    })
                    .build()

                transformer.start(composition, destFile.absolutePath)
            } catch (e: Exception) {
                Log.e("VideoProcessingEngine", "Failed to start transformer", e)
                failed = true
                latch.countDown()
            }
        }

        try {
            latch.await(300, TimeUnit.SECONDS) // Wait up to 5 minutes for full render
        } catch (_: InterruptedException) {}

        return completed && !failed && destFile.exists() && destFile.length() > 0L
    }

    private fun runDirectMediaMuxerClip(
        inputUri: Uri,
        startMs: Long,
        endMs: Long,
        destFile: File
    ): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        return try {
            extractor.setDataSource(context, inputUri, null)
            muxer = MediaMuxer(destFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackMap = HashMap<Int, Int>()
            var maxBufferSize = 1024 * 1024

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val newIndex = muxer.addTrack(format)
                    trackMap[i] = newIndex
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        val size = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        if (size > maxBufferSize) maxBufferSize = size
                    }
                }
            }

            muxer.start()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) break

                if (sampleTimeUs >= startUs) {
                    val trackIndex = extractor.sampleTrackIndex
                    val muxerTrackIndex = trackMap[trackIndex]

                    if (muxerTrackIndex != null) {
                        bufferInfo.offset = 0
                        bufferInfo.size = sampleSize
                        bufferInfo.presentationTimeUs = sampleTimeUs - startUs
                        var flags = 0
                        if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                            flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                        }
                        if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
                            flags = flags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
                        }
                        bufferInfo.flags = flags

                        muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    }
                }

                extractor.advance()
            }

            true
        } catch (e: Exception) {
            Log.e("VideoProcessingEngine", "Direct MediaMuxer clip failed", e)
            false
        } finally {
            try { extractor.release() } catch (_: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Exception) {}
        }
    }

    private fun verifyExportOutput(file: File): Boolean {
        if (!file.exists() || file.length() <= 0L) return false
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes"
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            hasVideo && durationMs > 0L
        } catch (e: Exception) {
            Log.e("VideoProcessingEngine", "Output verification failed for ${file.absolutePath}", e)
            false
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }
}
