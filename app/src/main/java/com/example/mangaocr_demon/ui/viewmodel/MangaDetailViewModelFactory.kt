package com.example.mangaocr_demon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mangaocr_demon.data.ChapterDao
import com.example.mangaocr_demon.data.MangaDao

class MangaDetailViewModelFactory(
    private val mangaDao: MangaDao,
    private val chapterDao: ChapterDao,
    private val mangaId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MangaDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MangaDetailViewModel(mangaDao, chapterDao, mangaId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
