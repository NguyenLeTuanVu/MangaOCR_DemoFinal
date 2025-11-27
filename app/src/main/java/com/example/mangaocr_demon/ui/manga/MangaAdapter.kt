package com.example.mangaocr_demon.ui.manga

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mangaocr_demon.R
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.data.MangaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

class MangaAdapter(
    private val onClick: (MangaEntity) -> Unit,
    private val onDeleteClick: (MangaEntity) -> Unit,
    private val onLongClick: ((MangaEntity) -> Unit)? = null
) : ListAdapter<MangaEntity, MangaAdapter.MangaViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<MangaEntity>() {
        override fun areItemsTheSame(oldItem: MangaEntity, newItem: MangaEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MangaEntity, newItem: MangaEntity): Boolean {
            return oldItem == newItem
        }
    }

    inner class MangaViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        private val coverImage: ImageView = itemView.findViewById(R.id.ivMangaCover)
        private val titleText: TextView = itemView.findViewById(R.id.tvMangaTitle)
        private val descText: TextView = itemView.findViewById(R.id.tvMangaDesc)
        private val chapterCountText: TextView = itemView.findViewById(R.id.tvChapterCount)
        private val pageCountText: TextView = itemView.findViewById(R.id.tvPageCount)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteManga)

        private var currentManga: MangaEntity? = null

        init {
            // Click vào card để mở manga
            itemView.setOnClickListener {
                currentManga?.let { onClick(it) }
            }

            // Long click để show options
            itemView.setOnLongClickListener {
                currentManga?.let { manga ->
                    onLongClick?.invoke(manga)
                    true
                } ?: false
            }

            // Click nút xóa
            deleteButton.setOnClickListener {
                currentManga?.let { manga ->
                    android.util.Log.d("MangaAdapter", "Delete button clicked for: ${manga.title}")
                    onDeleteClick(manga)
                }
            }
        }

        fun bind(manga: MangaEntity) {
            currentManga = manga
            titleText.text = manga.title
            descText.text = manga.description ?: "Không có mô tả"

            // ✅ Set default values first
            chapterCountText.text = "0 ch"
            pageCountText.text = "0 trang"

            // Load cover image
            if (!manga.coverImageUri.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(manga.coverImageUri)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(coverImage)
            } else {
                Glide.with(itemView.context)
                    .load(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(coverImage)
            }

            // ✅ Load real stats
            loadMangaStats(manga)
        }

        private fun loadMangaStats(manga: MangaEntity) {
            val context = itemView.context

            // Get lifecycle owner from context
            val lifecycleOwner = try {
                context as? LifecycleOwner
            } catch (e: Exception) {
                null
            }

            lifecycleOwner?.lifecycleScope?.launch {
                try {
                    val db = AppDatabase.getDatabase(context)

                    // ✅ Use first() to get data once
                    withContext(Dispatchers.IO) {
                        // Get chapters
                        val chapters = db.chapterDao().getChaptersForManga(manga.id).first()

                        val chapterCount = chapters.size

                        // Count total pages
                        var totalPages = 0
                        chapters.forEach { chapter ->
                            val pages = db.pageDao().getPagesForChapter(chapter.id).first()
                            totalPages += pages.size
                        }

                        // Update UI on main thread
                        withContext(Dispatchers.Main) {
                            chapterCountText.text = "$chapterCount ch"
                            pageCountText.text = "$totalPages trang"
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MangaAdapter", "Error loading stats", e)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manga, parent, false)
        return MangaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MangaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}