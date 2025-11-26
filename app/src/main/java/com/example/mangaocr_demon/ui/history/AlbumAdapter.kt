package com.example.mangaocr_demon.ui.history

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaocr_demon.data.AlbumEntity
import com.example.mangaocr_demon.databinding.ItemAlbumBinding

class AlbumAdapter(
    private val onAlbumClick: (AlbumEntity) -> Unit,
    private val onAlbumLongClick: (AlbumEntity) -> Unit,
    private val onDeleteClick: (AlbumEntity) -> Unit // ✅ NEW: Callback cho nút xóa
) : ListAdapter<AlbumEntity, AlbumAdapter.AlbumViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<AlbumEntity>() {
        override fun areItemsTheSame(oldItem: AlbumEntity, newItem: AlbumEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AlbumEntity, newItem: AlbumEntity): Boolean {
            return oldItem == newItem
        }
    }

    inner class AlbumViewHolder(
        private val binding: ItemAlbumBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(album: AlbumEntity) {
            binding.tvAlbumName.text = album.name
            binding.tvAlbumDescription.text = album.description ?: "Không có mô tả"

            // Set màu background
            try {
                binding.cardAlbum.setCardBackgroundColor(Color.parseColor(album.color))
            } catch (e: Exception) {
                binding.cardAlbum.setCardBackgroundColor(Color.parseColor("#4CAF50"))
            }

            // Click vào card để mở album
            binding.root.setOnClickListener {
                onAlbumClick(album)
            }

            // Long click
            binding.root.setOnLongClickListener {
                onAlbumLongClick(album)
                true
            }

            // ✅ NEW: Click nút xóa
            binding.btnDeleteAlbum.setOnClickListener {
                onDeleteClick(album)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val binding = ItemAlbumBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlbumViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}