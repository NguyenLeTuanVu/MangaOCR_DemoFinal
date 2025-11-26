package com.example.mangaocr_demon.ui.manga

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mangaocr_demon.R
import com.example.mangaocr_demon.data.MangaEntity

class MangaAdapter(
    private val onClick: (MangaEntity) -> Unit,
    private val onDeleteClick: (MangaEntity) -> Unit
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
            // ✅ Click vào card để mở manga
            itemView.setOnClickListener {
                currentManga?.let { onClick(it) }
            }

            // ✅ Click nút xóa
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
            chapterCountText.text = "0 ch"
            pageCountText.text = "0 trang"

            // ✅ Load cover image với placeholder đẹp
            if (!manga.coverImageUri.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(manga.coverImageUri)
                    .placeholder(R.drawable.ic_manga_placeholder)
                    .error(R.drawable.ic_manga_placeholder)
                    .centerCrop()
                    .into(coverImage)
            } else {
                // ✅ Nếu không có cover, dùng placeholder
                Glide.with(itemView.context)
                    .load(R.drawable.ic_manga_placeholder)
                    .centerCrop()
                    .into(coverImage)
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