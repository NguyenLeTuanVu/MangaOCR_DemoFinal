package com.example.mangaocr_demon.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        HistoryEntity::class,
        MangaEntity::class,
        ChapterEntity::class,
        PageEntity::class,
        AlbumEntity::class,        // ← THÊM
        AlbumChapterEntity::class
    ],
    version = 6, // 🔺 tăng version để Room build lại DB
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao
    abstract fun mangaDao(): MangaDao
    abstract fun chapterDao(): ChapterDao
    abstract fun pageDao(): PageDao
    abstract fun albumDao(): AlbumDao           // ← THÊM
    abstract fun albumChapterDao(): AlbumChapterDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null


        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "manga_database"
                )
                    .fallbackToDestructiveMigration() // ⚠️ Xóa data cũ, dùng migration nếu cần giữ
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}