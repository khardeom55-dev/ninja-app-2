package com.example.editor

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream

class MediaStoreExporter(private val context: Context) {

    fun saveVideoToNinjaFolder(sourceFile: File, displayName: String, subFolder: String = "Shorts"): File {
        val relativeSubPath = "NinjaAutoEditor/$subFolder"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$relativeSubPath")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri: Uri? = context.contentResolver.insert(collection, contentValues)

            if (uri != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(out)
                        }
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)

                    // Also keep a copy in app external storage for direct file path access
                    val appNinjaDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), relativeSubPath)
                    if (!appNinjaDir.exists()) appNinjaDir.mkdirs()
                    val targetAppFile = File(appNinjaDir, displayName)
                    sourceFile.copyTo(targetAppFile, overwrite = true)

                    return targetAppFile
                } catch (e: Exception) {
                    Log.e("MediaStoreExporter", "Error writing to MediaStore", e)
                }
            }
        }

        // Direct file system write for older API levels or fallback
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val ninjaPublicDir = File(moviesDir, relativeSubPath)
        if (!ninjaPublicDir.exists()) ninjaPublicDir.mkdirs()

        val destFile = File(ninjaPublicDir, displayName)
        sourceFile.copyTo(destFile, overwrite = true)
        return destFile
    }

    fun getExportFolder(subFolder: String = "Shorts"): File {
        val appNinjaDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "NinjaAutoEditor/$subFolder")
        if (!appNinjaDir.exists()) appNinjaDir.mkdirs()
        return appNinjaDir
    }
}

