package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [EditorSettings::class, MemeAsset::class, ExportProject::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun memeDao(): MemeDao
    abstract fun exportDao(): ExportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    lateinit var dbInstance: AppDatabase
                    dbInstance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "ninja_auto_editor.db"
                    )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed default settings on first run
                            CoroutineScope(Dispatchers.IO).launch {
                                dbInstance.settingsDao().insertOrUpdateSettings(EditorSettings())
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                    INSTANCE = dbInstance
                    dbInstance
                }
            }
        }
    }
}
