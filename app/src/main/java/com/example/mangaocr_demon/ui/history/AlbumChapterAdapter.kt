package com.example.mangaocr_demon.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaocr_demon.data.ChapterEntity
import com.example.mangaocr_demon.databinding.ItemChapterBinding

class AlbumChapterAdapter(
    private val onChapterClick: (ChapterEntity) -> Unit,
    private val onChapterLongClick: (ChapterEntity) -> Unit
) : ListAdapter<ChapterEntity, AlbumChapterAdapter.ChapterViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<ChapterEntity>() {
        override fun areItemsTheSame(oldItem: ChapterEntity, newItem: ChapterEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChapterEntity, newItem: ChapterEntity): Boolean {
            return oldItem == newItem
        }
    }

    inner class ChapterViewHolder(
        private val binding: ItemChapterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chapter: ChapterEntity) {
            // Hiá»ƒn thá»‹ title náº¿u cÃ³, náº¿u khÃ´ng thÃ¬ hiá»ƒn thá»‹ "No title"
            binding.tvChapterTitle.text = chapter.title ?: "No title"

            // Hiá»ƒn thá»‹ chapter number
            binding.tvChapterNumber.text = "Chapter ${chapter.number}"

            binding.root.setOnClickListener {
                onChapterClick(chapter)
            }

            binding.root.setOnLongClickListener {
                onChapterLongClick(chapter)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val binding = ItemChapterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}