package com.example.mangaocr_demon.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mangaocr_demon.data.ChapterDao
import com.example.mangaocr_demon.data.PageDao

class ChapterReaderViewModelFactory(
    private val pageDao: PageDao,
    private val chapterDao: ChapterDao,
    private val chapterId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChapterReaderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChapterReaderViewModel(pageDao, chapterDao, chapterId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}