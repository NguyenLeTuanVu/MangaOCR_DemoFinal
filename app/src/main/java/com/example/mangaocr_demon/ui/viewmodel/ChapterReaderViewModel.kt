package com.example.mangaocr_demon.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.mangaocr_demon.data.ChapterDao
import com.example.mangaocr_demon.data.ChapterEntity
import com.example.mangaocr_demon.data.PageDao
import com.example.mangaocr_demon.data.PageEntity
import kotlinx.coroutines.launch

class ChapterReaderViewModel(
    private val pageDao: PageDao,
    private val chapterDao: ChapterDao,
    private val chapterId: Long
) : ViewModel() {

    val chapter: LiveData<ChapterEntity?> = chapterDao.getChapterById(chapterId).asLiveData()

    // ⭐ Use MutableLiveData for manual triggering
    private val _pages = MutableLiveData<List<PageEntity>>()
    val pages: LiveData<List<PageEntity>> = _pages

    init {
        // Initial load
        loadPages()
    }

    private fun loadPages() {
        viewModelScope.launch {
            pageDao.getPagesByChapterId(chapterId).collect { pageList ->
                android.util.Log.d("ChapterReaderViewModel", "📄 Pages updated: ${pageList.size} pages")
                pageList.forEachIndexed { index, page ->
                    android.util.Log.d("ChapterReaderViewModel",
                        "  Page $index (id=${page.id}): isOcrProcessed=${page.isOcrProcessed}, " +
                                "ocrDataJson length=${page.ocrDataJson?.length ?: 0}")
                }
                _pages.value = pageList
            }
        }
    }

    // ⭐ NEW: Manual refresh method
    fun refreshPages() {
        android.util.Log.d("ChapterReaderViewModel", "🔄 Manual refresh triggered")
        viewModelScope.launch {
            val freshPages = pageDao.getPagesByChapterIdSync(chapterId)
            android.util.Log.d("ChapterReaderViewModel", "📄 Loaded ${freshPages.size} fresh pages from DB")
            _pages.postValue(freshPages)
        }
    }
}