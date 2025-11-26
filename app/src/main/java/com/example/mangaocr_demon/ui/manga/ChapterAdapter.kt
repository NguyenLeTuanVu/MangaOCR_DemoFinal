package com.example.mangaocr_demon.ui.manga

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaocr_demon.data.ChapterEntity
import com.example.mangaocr_demon.databinding.ItemChapterBinding

class ChapterAdapter(
    private val onClick: (ChapterEntity) -> Unit
) : ListAdapter<ChapterEntity, ChapterAdapter.ChapterViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<ChapterEntity>() {
        override fun areItemsTheSame(oldItem: ChapterEntity, newItem: ChapterEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChapterEntity, newItem: ChapterEntity): Boolean {
            return oldItem == newItem
        }
    }

    class ChapterViewHolder(
        private val binding: ItemChapterBinding,
        private val onClick: (ChapterEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private var currentChapter: ChapterEntity? = null

        init {
            binding.root.setOnClickListener {
                currentChapter?.let { onClick(it) }
            }
        }

        fun bind(chapter: ChapterEntity) {
            currentChapter = chapter
            binding.tvChapterTitle.text = "Chapter ${chapter.number}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val binding = ItemChapterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChapterViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
