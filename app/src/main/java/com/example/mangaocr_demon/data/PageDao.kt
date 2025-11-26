package com.example.mangaocr_demon.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {
    @Insert
    suspend fun insert(page: PageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pages: List<PageEntity>)

    @Update
    suspend fun update(page: PageEntity)

    @Delete
    suspend fun delete(page: PageEntity)

    @Query("SELECT * FROM page WHERE chapter_id = :chapterId ORDER BY page_index ASC")
    fun getPagesForChapter(chapterId: Long): Flow<List<PageEntity>>

    @Query("SELECT * FROM page WHERE id = :id LIMIT 1")
    fun getPageById(id: Long): Flow<PageEntity?>

    @Query("DELETE FROM page WHERE chapter_id = :chapterId")
    suspend fun deletePagesForChapter(chapterId: Long)

    @Query("SELECT * FROM page WHERE chapter_id = :chapterId ORDER BY page_index ASC")
    fun getPagesByChapterId(chapterId: Long): Flow<List<PageEntity>>


    @Query("""
        UPDATE page 
        SET ocr_data_json = :ocrDataJson,
            is_ocr_processed = :isProcessed,
            ocr_language = :language,
            last_ocr_date = :timestamp
        WHERE id = :pageId
    """)
    suspend fun updateOcrData(
        pageId: Long,
        ocrDataJson: String,
        isProcessed: Boolean,
        language: String,
        timestamp: Long
    )


    @Query("SELECT * FROM page WHERE id = :pageId")
    suspend fun getPageByIdSync(pageId: Long): PageEntity?

    @Query("""
        UPDATE page 
        SET ocr_data_json = NULL,
            is_ocr_processed = 0,
            ocr_language = NULL,
            last_ocr_date = NULL
        WHERE id = :pageId
    """)
    suspend fun resetOcrData(pageId: Long)

    @Query("""
        UPDATE page 
        SET ocr_data_json = NULL,
            is_ocr_processed = 0,
            ocr_language = NULL,
            last_ocr_date = NULL
        WHERE chapter_id = :chapterId
    """)
    suspend fun resetOcrDataForChapter(chapterId: Long)

    @Query("SELECT * FROM page WHERE chapter_id = :chapterId ORDER BY page_index ASC")
    suspend fun getPagesByChapterIdSync(chapterId: Long): List<PageEntity>
}