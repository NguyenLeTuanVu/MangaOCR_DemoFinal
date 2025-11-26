package com.example.mangaocr_demon.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "album_chapter",
    primaryKeys = ["album_id", "chapter_id"],
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,

            parentColumns = arrayOf("id"),
            childColumns = arrayOf("album_id"),
            onDelete = ForeignKey.Companion.CASCADE
        ),
        ForeignKey(
            entity = ChapterEntity::class,

            parentColumns = arrayOf("id"),
            childColumns = arrayOf("chapter_id"),
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],

    indices = [
        Index(value = arrayOf("album_id")),
        Index(value = arrayOf("chapter_id"))
    ]
)
data class AlbumChapterEntity(
    @ColumnInfo(name = "album_id") val albumId: Long,
    @ColumnInfo(name = "chapter_id") val chapterId: Long,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
)