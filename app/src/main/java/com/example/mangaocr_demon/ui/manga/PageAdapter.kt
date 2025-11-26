// File: ui/manga/PageAdapter.kt
package com.example.mangaocr_demon.ui.manga

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mangaocr_demon.R
import com.example.mangaocr_demon.data.PageEntity
import com.example.mangaocr_demon.data.model.TextBlock
import com.example.mangaocr_demon.data.model.OcrData
import com.example.mangaocr_demon.databinding.ItemPageBinding
import com.ymg.pdf.viewer.PDFView
import kotlinx.serialization.json.Json

class PageAdapter(
    private val onPageLongClick: (PageEntity) -> Unit,
    private val onTextBlockClick: ((PageEntity, TextBlock) -> Unit)? = null
) : ListAdapter<PageEntity, PageAdapter.PageViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<PageEntity>() {
        override fun areItemsTheSame(oldItem: PageEntity, newItem: PageEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PageEntity, newItem: PageEntity): Boolean {
            return oldItem.id == newItem.id &&
                    oldItem.imageUri == newItem.imageUri &&
                    oldItem.pageType == newItem.pageType &&
                    oldItem.isOcrProcessed == newItem.isOcrProcessed &&
                    oldItem.ocrDataJson == newItem.ocrDataJson &&
                    oldItem.translatedText == newItem.translatedText
        }

        override fun getChangePayload(oldItem: PageEntity, newItem: PageEntity): Any? {
            if (oldItem.ocrDataJson != newItem.ocrDataJson) {
                return "OCR_UPDATED"
            }
            return null
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    class PageViewHolder(
        private val binding: ItemPageBinding,
        private val onPageLongClick: (PageEntity) -> Unit,
        private val onTextBlockClick: ((PageEntity, TextBlock) -> Unit)?,
        private val json: Json
    ) : RecyclerView.ViewHolder(binding.root) {
        private var currentPage: PageEntity? = null

        init {
            binding.root.setOnLongClickListener {
                currentPage?.let {
                    onPageLongClick(it)
                    true
                } ?: false
            }
        }

        fun bind(page: PageEntity) {
            currentPage = page

            // Reset all views
            binding.imageView.visibility = View.GONE
            binding.pdfView.visibility = View.GONE
            binding.loadingIndicator.visibility = View.GONE
            binding.errorText.visibility = View.GONE

            when (page.pageType) {
                "PDF" -> bindPdfPage(page)
                "IMAGE" -> bindImagePage(page)
                else -> bindImagePage(page)
            }

            // Show indicators
            binding.ocrIndicator.visibility = if (page.isOcrProcessed) {
                View.VISIBLE
            } else {
                View.GONE
            }

            binding.translationIndicator.visibility = if (!page.translatedText.isNullOrEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Load OCR overlay
            loadOcrOverlay(page)
        }

        fun bind(page: PageEntity, payloads: List<Any>) {
            currentPage = page

            if (payloads.contains("OCR_UPDATED")) {
                loadOcrOverlay(page)
                binding.ocrIndicator.visibility = if (page.isOcrProcessed) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            } else {
                bind(page)
            }
        }

        private fun bindImagePage(page: PageEntity) {
            binding.imageView.visibility = View.VISIBLE

            try {
                val uri = page.imageUri?.let { Uri.parse(it) }
                Glide.with(binding.root.context)
                    .load(uri)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(binding.imageView)
            } catch (e: Exception) {
                binding.imageView.setImageResource(R.drawable.ic_image_error)
            }
        }

        private fun bindPdfPage(page: PageEntity) {
            binding.pdfView.visibility = View.VISIBLE
            binding.loadingIndicator.visibility = View.VISIBLE

            try {
                val pdfUri = page.pdfUri
                val pageNumber = page.pdfPageNumber ?: 0

                if (pdfUri.isNullOrEmpty()) {
                    showError("PDF URI is empty")
                    return
                }

                val uri = Uri.parse(pdfUri)
                binding.pdfView.fromUri(uri)
                    .defaultPage(pageNumber)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .spacing(10)
                    .onLoad { numPages ->
                        binding.loadingIndicator.visibility = View.GONE
                    }
                    .onError { error ->
                        binding.loadingIndicator.visibility = View.GONE
                        showError("PDF Error: ${error.message}")
                    }
                    .load()
            } catch (e: Exception) {
                binding.loadingIndicator.visibility = View.GONE
                showError("Exception: ${e.message}")
            }
        }

        // File: ui/manga/PageAdapter.kt

        private fun loadOcrOverlay(page: PageEntity) {
            if (page.ocrDataJson.isNullOrEmpty()) {
                binding.overlayView.setTextBlocks(emptyList(), 0, 0)
                return
            }

            try {
                val ocrData = json.decodeFromString<OcrData>(page.ocrDataJson)

                // ⭐ Wait for ImageView to load, then sync overlay
                binding.imageView.post {
                    val imageViewWidth = binding.imageView.width
                    val imageViewHeight = binding.imageView.height

                    android.util.Log.d("PageAdapter", "ImageView size: ${imageViewWidth}x${imageViewHeight}")
                    android.util.Log.d("PageAdapter", "OCR image size: ${ocrData.imageWidth}x${ocrData.imageHeight}")

                    // ⭐ Calculate actual display dimensions considering fitCenter
                    val imageAspect = ocrData.imageWidth.toFloat() / ocrData.imageHeight
                    val viewAspect = imageViewWidth.toFloat() / imageViewHeight

                    var displayWidth = imageViewWidth
                    var displayHeight = imageViewHeight

                    if (imageAspect > viewAspect) {
                        // Image is wider - fit to width
                        displayHeight = (imageViewWidth / imageAspect).toInt()
                    } else {
                        // Image is taller - fit to height
                        displayWidth = (imageViewHeight * imageAspect).toInt()
                    }

                    android.util.Log.d("PageAdapter", "Calculated display size: ${displayWidth}x${displayHeight}")

                    binding.overlayView.setTextBlocks(
                        ocrData.textBlocks,
                        displayWidth, // ⭐ Use calculated dimensions
                        displayHeight
                    )
                }

                binding.overlayView.setOnTextBlockClickListener { textBlock ->
                    currentPage?.let { onTextBlockClick?.invoke(it, textBlock) }
                }
            } catch (e: Exception) {
                android.util.Log.e("PageAdapter", "Error loading overlay", e)
            }
        }

        private fun showError(message: String) {
            binding.pdfView.visibility = View.GONE
            binding.errorText.visibility = View.VISIBLE
            binding.errorText.text = message
        }

        fun cleanup() {
            try {
                binding.pdfView.recycle()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PageViewHolder(binding, onPageLongClick, onTextBlockClick, json)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            holder.bind(getItem(position), payloads)
        }
    }

    override fun onViewRecycled(holder: PageViewHolder) {
        super.onViewRecycled(holder)
        holder.cleanup()
    }
}