package com.example.mangaocr_demon.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "page",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chapter_id"])]
)
data class PageEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @ColumnInfo(name = "chapter_id") val chapterId: Long,
    @ColumnInfo(name = "page_index") val pageIndex: Int,
    @ColumnInfo(name = "image_uri") val imageUri: String? = null,

    // ⭐ DEPRECATED - Giữ để tương thích, nhưng dùng ocrDataJson
    @ColumnInfo(name = "ocr_text") val ocrText: String? = null,
    @ColumnInfo(name = "translated_text") val translatedText: String? = null,

    // ⭐ MỚI - Lưu toàn bộ text blocks với vị trí
    @ColumnInfo(name = "ocr_data_json") val ocrDataJson: String? = null,
    @ColumnInfo(name = "is_ocr_processed") val isOcrProcessed: Boolean = false,
    @ColumnInfo(name = "ocr_language") val ocrLanguage: String? = null, // "zh", "en", "ja"
    @ColumnInfo(name = "last_ocr_date") val lastOcrDate: Long? = null,

    @ColumnInfo(name = "pdf_uri") val pdfUri: String? = null,
    @ColumnInfo(name = "pdf_page_number") val pdfPageNumber: Int? = null,
    @ColumnInfo(name = "page_type") val pageType: String = "IMAGE",
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
)