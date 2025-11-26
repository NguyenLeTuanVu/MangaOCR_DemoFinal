// File: ui/history/HistoryAdapter.kt
package com.example.mangaocr_demon.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mangaocr_demon.R
import com.example.mangaocr_demon.data.HistoryEntity
import com.example.mangaocr_demon.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val onItemClick: ((HistoryEntity) -> Unit)? = null, // ⭐ NEW
    private val onDeleteClick: ((HistoryEntity) -> Unit)? = null // ⭐ NEW
) : ListAdapter<HistoryEntity, HistoryAdapter.HistoryViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<HistoryEntity>() {
        override fun areItemsTheSame(oldItem: HistoryEntity, newItem: HistoryEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HistoryEntity, newItem: HistoryEntity): Boolean {
            return oldItem == newItem
        }
    }

    inner class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // ⭐ Click listeners
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(getItem(position))
                }
            }

            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick?.invoke(getItem(position))
                    true
                } else false
            }
        }

        fun bind(item: HistoryEntity) {
            // OCR text (original)
            binding.tvOCR.text = item.ocrText

            // Translated text
            binding.tvTranslated.text = item.translatedText

            // ⭐ Format timestamp
            binding.tvTime.text = formatTimestamp(item.timestamp)

            // ⭐ Show manga info if available
            if (!item.mangaTitle.isNullOrEmpty()) {
                binding.tvOCR.text = buildString {
                    append(item.mangaTitle)
                    if (!item.chapterNumber.isNullOrEmpty()) {
                        append(" - Ch.${item.chapterNumber}")
                    }
                    append("\n")
                    append(item.ocrText)
                }
            }

            // Load image
            Glide.with(binding.root.context)
                .load(item.imageUri)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .centerCrop()
                .into(binding.imageView)
        }

        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}