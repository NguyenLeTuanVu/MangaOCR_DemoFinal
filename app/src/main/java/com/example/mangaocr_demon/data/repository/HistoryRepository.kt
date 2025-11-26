// File: data/repository/HistoryRepository.kt
package com.example.mangaocr_demon.data.repository

import com.example.mangaocr_demon.data.HistoryDao
import com.example.mangaocr_demon.data.HistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryRepository(private val historyDao: HistoryDao) {

    val allHistory = historyDao.getAllHistory()

    suspend fun insert(history: HistoryEntity) = withContext(Dispatchers.IO) {
        historyDao.insert(history)
        android.util.Log.d("HistoryRepository", "âœ… Saved history: ${history.ocrText.take(50)}")
    }

    suspend fun insertAll(histories: List<HistoryEntity>) = withContext(Dispatchers.IO) {
        historyDao.insertAll(histories)
        android.util.Log.d("HistoryRepository", "âœ… Saved ${histories.size} history items")
    }

    suspend fun deleteById(id: Int) = withContext(Dispatchers.IO) {
        historyDao.deleteById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        historyDao.clearAll()
    }

    fun getRecentHistory(limit: Int) = historyDao.getRecentHistory(limit)

    fun searchHistory(query: String) = historyDao.searchHistory(query)
}