// File: ui/reader/TextBlockDetailDialog.kt
package com.example.mangaocr_demon.ui.reader

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.example.mangaocr_demon.R
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.data.model.OcrData
import com.example.mangaocr_demon.data.model.TextBlock
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class TextBlockDetailDialog : DialogFragment() {

    var onDismissListener: (() -> Unit)? = null

    private var pageId: Long = -1
    private lateinit var textBlock: TextBlock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pageId = it.getLong(ARG_PAGE_ID)
            val textBlockJson = it.getString(ARG_TEXT_BLOCK)
            textBlock = Json.decodeFromString(textBlockJson ?: "")
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Gọi hàm listener nếu nó đã được thiết lập
        onDismissListener?.invoke()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_text_block_detail, null)

        setupViews(view)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Translation")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                saveChanges(view)
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Copy") { _, _ ->
                copyToClipboard()
            }
            .create()
    }

    private fun setupViews(view: android.view.View) {
        val tvOriginalText = view.findViewById<TextView>(R.id.tvOriginalText)
        val etEditText = view.findViewById<EditText>(R.id.etEditText)
        val tvLanguage = view.findViewById<TextView>(R.id.tvLanguage)
        val tvConfidence = view.findViewById<TextView>(R.id.tvConfidence)

        tvOriginalText.text = textBlock.originalText

        // ⭐ FIX: Show translation if available, otherwise show original
        val displayText = if (textBlock.translatedText.isNotEmpty()) {
            textBlock.translatedText
        } else {
            textBlock.originalText
        }
        etEditText.setText(displayText)

        tvLanguage.text = "Language: ${getLanguageName(textBlock.language)}"
        tvConfidence.text = "Confidence: ${(textBlock.confidence * 100).toInt()}%"

        // ⭐ NEW: Show translation status
        if (textBlock.translatedText.isNotEmpty()) {
            tvLanguage.append(" → Vietnamese (Translated)")
        }
    }

    private fun saveChanges(view: android.view.View) {
        val etEditText = view.findViewById<EditText>(R.id.etEditText)
        val newText = etEditText.text.toString()

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val page = db.pageDao().getPageByIdSync(pageId)

                if (page?.ocrDataJson != null) {
                    // Parse existing OCR data
                    val ocrData = Json.decodeFromString<OcrData>(page.ocrDataJson)

                    // Find and update the text block
                    val updatedBlocks = ocrData.textBlocks.map { block ->
                        if (block.id == textBlock.id) {
                            // ⭐ Update translated text, mark as manually edited
                            block.copy(
                                translatedText = newText,
                                isManuallyEdited = true
                            )
                        } else {
                            block
                        }
                    }

                    // Save back to database
                    val updatedOcrData = ocrData.copy(textBlocks = updatedBlocks)
                    val jsonData = Json.encodeToString(updatedOcrData)

                    db.pageDao().updateOcrData(
                        pageId = pageId,
                        ocrDataJson = jsonData,
                        isProcessed = true,
                        language = page.ocrLanguage ?: "unknown",
                        timestamp = System.currentTimeMillis()
                    )

                    android.util.Log.d("TextBlockDialog", "✅ Updated translation for block ${textBlock.id}")

                    // Notify success
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Translation updated",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()

                    // TODO: Notify parent to refresh view
                }
            } catch (e: Exception) {
                android.util.Log.e("TextBlockDialog", "Failed to update translation", e)
                android.widget.Toast.makeText(
                    requireContext(),
                    "Failed to save: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun copyToClipboard() {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager

        // ⭐ Copy translation if available
        val textToCopy = if (textBlock.translatedText.isNotEmpty()) {
            textBlock.translatedText
        } else {
            textBlock.originalText
        }

        val clip = android.content.ClipData.newPlainText("Text", textToCopy)
        clipboard.setPrimaryClip(clip)

        android.widget.Toast.makeText(
            requireContext(),
            "Copied to clipboard",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun getLanguageName(code: String): String {
        return when (code) {
            "zh" -> "Chinese (中文)"
            "en" -> "English"
            "ja" -> "Japanese (日本語)"
            "vi" -> "Vietnamese (Tiếng Việt)"
            else -> code
        }
    }

    companion object {
        private const val ARG_PAGE_ID = "page_id"
        private const val ARG_TEXT_BLOCK = "text_block"

        fun newInstance(pageId: Long, textBlock: TextBlock): TextBlockDetailDialog {
            return TextBlockDetailDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PAGE_ID, pageId)
                    putString(ARG_TEXT_BLOCK, Json.encodeToString(textBlock))
                }
            }
        }
    }
}