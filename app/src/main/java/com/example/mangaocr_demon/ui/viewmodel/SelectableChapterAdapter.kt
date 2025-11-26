package com.example.mangaocr_demon.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaocr_demon.data.ChapterEntity
import com.example.mangaocr_demon.databinding.ItemChapterSelectableBinding

class SelectableChapterAdapter : ListAdapter<ChapterEntity, SelectableChapterAdapter.ViewHolder>(DiffCallback) {

    private val selectedChapters = mutableSetOf<Long>()
    var onSelectionChanged: ((Int) -> Unit)? = null

    companion object DiffCallback : DiffUtil.ItemCallback<ChapterEntity>() {
        override fun areItemsTheSame(oldItem: ChapterEntity, newItem: ChapterEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChapterEntity, newItem: ChapterEntity): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(
        private val binding: ItemChapterSelectableBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chapter: ChapterEntity) {
            binding.tvChapterNumber.text = "Chapter ${chapter.number}"
            binding.tvChapterTitle.text = chapter.title ?: "No title"

            // Set checkbox state
            binding.cbSelectChapter.isChecked = selectedChapters.contains(chapter.id)

            // Handle checkbox changes
            binding.cbSelectChapter.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedChapters.add(chapter.id)
                } else {
                    selectedChapters.remove(chapter.id)
                }
                onSelectionChanged?.invoke(selectedChapters.size)
            }

            // Handle item click (toggle checkbox)
            binding.root.setOnClickListener {
                binding.cbSelectChapter.isChecked = !binding.cbSelectChapter.isChecked
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChapterSelectableBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getSelectedChapters(): List<ChapterEntity> {
        return currentList.filter { selectedChapters.contains(it.id) }
    }

    override fun submitList(list: List<ChapterEntity>?) {
        // Clear selections when new list is loaded
        selectedChapters.clear()
        onSelectionChanged?.invoke(0)
        super.submitList(list)
    }
}