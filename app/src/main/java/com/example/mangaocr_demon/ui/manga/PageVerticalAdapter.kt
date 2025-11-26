package com.example.mangaocr_demon.ui.manga

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mangaocr_demon.data.PageEntity
import com.example.mangaocr_demon.databinding.ItemPageVerticalBinding

class PageVerticalAdapter(
    private val onPageClick: (PageEntity) -> Unit
) : ListAdapter<PageEntity, PageVerticalAdapter.PageViewHolder>(PageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPageVerticalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PageViewHolder(binding, onPageClick)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PageViewHolder(
        private val binding: ItemPageVerticalBinding,
        private val onPageClick: (PageEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(page: PageEntity) {
            // Load image - xử lý cả IMAGE và PDF
            val imageSource = when (page.pageType) {
                "PDF" -> page.pdfUri
                else -> page.imageUri
            }

            Glide.with(binding.root.context)
                .load(imageSource)
                .into(binding.ivPage)

            // Click listener for OCR/translate
            binding.root.setOnLongClickListener {
                onPageClick(page)
                true
            }
        }
    }

    private class PageDiffCallback : DiffUtil.ItemCallback<PageEntity>() {
        override fun areItemsTheSame(oldItem: PageEntity, newItem: PageEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PageEntity, newItem: PageEntity): Boolean {
            return oldItem == newItem
        }
    }
}