package com.example.mangaocr_demon.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    // Thêm album mới
    @Insert
    suspend fun insert(album: AlbumEntity): Long

    // Cập nhật thông tin album
    @Update
    suspend fun update(album: AlbumEntity)

    // Xóa album
    @Delete
    suspend fun delete(album: AlbumEntity)

    // Lấy tất cả album, sắp xếp theo thời gian tạo (mới nhất trước)
    @Query("SELECT * FROM album ORDER BY created_at DESC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    // Lấy một album theo id
    @Query("SELECT * FROM album WHERE id = :albumId")
    fun getAlbumById(albumId: Long): Flow<AlbumEntity?>

    // Lấy danh sách tất cả các chapter trong một album
    @Query("""
        SELECT chapter.* FROM chapter 
        INNER JOIN album_chapter ON chapter.id = album_chapter.chapter_id 
        WHERE album_chapter.album_id = :albumId 
        ORDER BY album_chapter.added_at DESC
    """)
    fun getChaptersInAlbum(albumId: Long): Flow<List<ChapterEntity>>

    // Đếm số chapter trong album
    @Query("SELECT COUNT(*) FROM album_chapter WHERE album_id = :albumId")
    fun getChapterCount(albumId: Long): Flow<Int>
}

@Dao
interface AlbumChapterDao {
    // Thêm chapter vào album (bỏ qua nếu đã tồn tại)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addChapterToAlbum(albumChapter: AlbumChapterEntity)

    // Xóa chapter khỏi album
    @Delete
    suspend fun removeChapterFromAlbum(albumChapter: AlbumChapterEntity)

    // Xóa chapter khỏi album bằng id
    @Query("DELETE FROM album_chapter WHERE album_id = :albumId AND chapter_id = :chapterId")
    suspend fun removeChapterFromAlbumById(albumId: Long, chapterId: Long)

    // Kiểm tra chapter có thuộc album không
    @Query("SELECT * FROM album_chapter WHERE album_id = :albumId AND chapter_id = :chapterId")
    suspend fun isChapterInAlbum(albumId: Long, chapterId: Long): AlbumChapterEntity?
}