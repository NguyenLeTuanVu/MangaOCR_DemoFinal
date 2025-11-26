package com.example.mangaocr_demon.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Insert
    suspend fun insert(chapter: ChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<ChapterEntity>)

    @Update
    suspend fun update(chapter: ChapterEntity)

    @Delete
    suspend fun delete(chapter: ChapterEntity)

    @Query("SELECT * FROM chapter WHERE manga_id = :mangaId ORDER BY number ASC")
    fun getChaptersForManga(mangaId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapter WHERE id = :chapterId")
    fun getChapterById(chapterId: Long): Flow<ChapterEntity?>
}