// File: data/HistoryDao.kt
package com.example.mangaocr_demon.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity)

    // ⭐ NEW: Batch insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<HistoryEntity>)

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): LiveData<List<HistoryEntity>>

    // ⭐ NEW: Flow version for better reactivity
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistoryFlow(): Flow<List<HistoryEntity>>

    // ⭐ NEW: Get history by page
    @Query("SELECT * FROM history WHERE page_id = :pageId ORDER BY timestamp DESC")
    fun getHistoryByPage(pageId: Long): LiveData<List<HistoryEntity>>

    // ⭐ NEW: Get recent history (limit)
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): LiveData<List<HistoryEntity>>

    // ⭐ NEW: Search history
    @Query("""
        SELECT * FROM history 
        WHERE ocr_text LIKE '%' || :query || '%' 
           OR translated_text LIKE '%' || :query || '%'
           OR manga_title LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchHistory(query: String): LiveData<List<HistoryEntity>>

    // ⭐ NEW: Delete single item
    @Query("DELETE FROM history WHERE id = :historyId")
    suspend fun deleteById(historyId: Int)

    @Query("DELETE FROM history")
    suspend fun clearAll()

    // ⭐ NEW: Get count
    @Query("SELECT COUNT(*) FROM history")
    suspend fun getCount(): Int
}