
package com.example.mangaocr_demon.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    @ColumnInfo(name = "image_uri") val imageUri: String?, // URI ảnh gốc

    @ColumnInfo(name = "ocr_text") val ocrText: String, // Text OCR gốc

    @ColumnInfo(name = "translated_text") val translatedText: String, // Text đã dịch

    @ColumnInfo(name = "source_language") val sourceLanguage: String = "unknown", // ⭐ NEW: zh, en, ja

    @ColumnInfo(name = "target_language") val targetLanguage: String = "vi", // ⭐ NEW

    @ColumnInfo(name = "page_id") val pageId: Long? = null, // ⭐ NEW: Link to page

    @ColumnInfo(name = "manga_title") val mangaTitle: String? = null, // ⭐ NEW: Tên truyện

    @ColumnInfo(name = "chapter_number") val chapterNumber: String? = null, // ⭐ NEW: Chapter

    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(), // ⭐ Changed to Long

    @ColumnInfo(name = "translation_method") val translationMethod: String = "Gemini" // ⭐ NEW: Gemini/ChatGPT
)