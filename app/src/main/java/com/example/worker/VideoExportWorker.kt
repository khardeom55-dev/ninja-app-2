package com.example.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.*
import com.example.editor.VideoProcessingEngine
import java.io.File

class VideoExportWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = AppDatabase.getInstance(context)
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "ninja_export_channel"
        const val NOTIFICATION_ID = 8855
        const val KEY_PROJECT_ID = "key_project_id"
    }

    override suspend fun doWork(): Result {
        val projectId = inputData.getLong(KEY_PROJECT_ID, -1L)
        if (projectId == -1L) {
            return Result.failure()
        }

        val project = db.exportDao().getProjectById(projectId) ?: return Result.failure()
        val settings = db.settingsDao().getSettings() ?: EditorSettings()
        val memes = db.memeDao().getAllMemes()

        createNotificationChannel()
        setForeground(createForegroundInfo(project.title, 0))

        db.exportDao().updateProgress(projectId, 0, ExportStatus.PROCESSING)

        return try {
            val engine = VideoProcessingEngine(context)
            val sourceUri = Uri.parse(project.sourceVideoUri)

            val exportedFiles = engine.processAndExport(
                inputUri = sourceUri,
                outputMode = project.outputType,
                quality = project.quality,
                settings = settings,
                memes = memes,
                listener = object : VideoProcessingEngine.ProgressListener {
                    override fun onProgress(percent: Int) {
                        kotlinx.coroutines.runBlocking {
                            db.exportDao().updateProgress(projectId, percent, ExportStatus.PROCESSING)
                        }
                        notificationManager.notify(
                            NOTIFICATION_ID,
                            buildNotification(project.title, percent)
                        )
                        setProgressAsync(workDataOf("progress" to percent))
                    }
                }
            )

            if (exportedFiles.isNotEmpty()) {
                val primaryOutputFile = exportedFiles.first()
                db.exportDao().updateProgress(
                    id = projectId,
                    progressPercent = 100,
                    status = ExportStatus.COMPLETED,
                    filePath = primaryOutputFile.absolutePath
                )
                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildCompleteNotification(project.title, primaryOutputFile.name)
                )
                Result.success(workDataOf("output_path" to primaryOutputFile.absolutePath))
            } else {
                val errorMsg = "No video clips were produced. Please check source file accessibility."
                db.exportDao().updateProgress(projectId, 0, ExportStatus.FAILED, errorMsg = errorMsg)
                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildFailedNotification(project.title, errorMsg)
                )
                Result.failure(workDataOf("error" to errorMsg))
            }
        } catch (e: Exception) {
            Log.e("VideoExportWorker", "Worker export error", e)
            val errorMsg = e.localizedMessage ?: "Video export encountered an error."
            db.exportDao().updateProgress(projectId, 0, ExportStatus.FAILED, errorMsg = errorMsg)
            notificationManager.notify(
                NOTIFICATION_ID,
                buildFailedNotification(project.title, errorMsg)
            )
            Result.failure(workDataOf("error" to errorMsg))
        }
    }

    private fun createForegroundInfo(title: String, progress: Int): ForegroundInfo {
        val notification = buildNotification(title, progress)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ninja Auto Editor Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows video generation progress"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, progress: Int): Notification {
        val stageText = getStageText(progress)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Exporting: $title ($progress%)")
            .setContentText("Stage: $stageText")
            .setSmallIcon(android.R.drawable.ic_menu_upload_you_tube)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun getStageText(percent: Int): String {
        return when {
            percent <= 5 -> "Validation"
            percent <= 15 -> "Copying source video"
            percent <= 25 -> "Reading metadata"
            percent <= 40 -> "Analyzing audio & activity"
            percent <= 50 -> "Selecting highlights"
            percent <= 85 -> "Rendering video clips"
            percent <= 92 -> "Applying intro, outro, memes & audio"
            percent <= 98 -> "Saving & verifying output"
            else -> "Completed"
        }
    }

    private fun buildCompleteNotification(title: String, fileName: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Ninja Auto Edit Finished! 🎉")
            .setContentText("Saved $fileName to NinjaAutoEditor folder")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setAutoCancel(true)
            .build()
    }

    private fun buildFailedNotification(title: String, errorMsg: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Export Failed")
            .setContentText(errorMsg)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .build()
    }
}
