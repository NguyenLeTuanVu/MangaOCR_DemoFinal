package com.example.mangaocr_demon.viewmodel

import androidx.lifecycle.*
import com.example.mangaocr_demon.data.ChapterDao
import com.example.mangaocr_demon.data.MangaDao
import com.example.mangaocr_demon.data.ChapterEntity
import com.example.mangaocr_demon.data.MangaEntity
import kotlinx.coroutines.flow.map

class MangaDetailViewModel(
    private val mangaDao: MangaDao,
    private val chapterDao: ChapterDao,
    private val mangaId: Long
) : ViewModel() {

    val manga: LiveData<MangaEntity?> =
        mangaDao.getMangaById(mangaId).asLiveData()

    val chapters: LiveData<List<ChapterEntity>> =
        chapterDao.getChaptersForManga(mangaId).asLiveData()
}