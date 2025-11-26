package com.example.mangaocr_demon.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.mangaocr_demon.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddMangaDialogFragment : DialogFragment() {

    private var onConfirmListener: ((String, String) -> Unit)? = null
    private var sourceType: String = "IMAGE" // "IMAGE" hoặc "PDF"

    companion object {
        private const val ARG_SOURCE_TYPE = "source_type"

        fun newInstance(sourceType: String): AddMangaDialogFragment {
            return AddMangaDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SOURCE_TYPE, sourceType)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sourceType = arguments?.getString(ARG_SOURCE_TYPE) ?: "IMAGE"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_manga, null)

        val etMangaTitle = view.findViewById<EditText>(R.id.etMangaTitle)
        val etMangaDescription = view.findViewById<EditText>(R.id.etMangaDescription)

        // Set placeholder text dựa vào source type
        val placeholderTitle = if (sourceType == "PDF") {
            "Nhập tên manga (từ PDF)"
        } else {
            "Nhập tên manga (từ ảnh)"
        }
        etMangaTitle.hint = placeholderTitle

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Thêm manga mới")
            .setView(view)
            .setPositiveButton("Thêm") { _, _ ->
                val title = etMangaTitle.text.toString().trim()
                val description = etMangaDescription.text.toString().trim()

                if (title.isEmpty()) {
                    // Nếu không nhập tên, dùng tên mặc định
                    val defaultTitle = if (sourceType == "PDF") {
                        "Manga mới (PDF)"
                    } else {
                        "Manga mới (ảnh)"
                    }
                    onConfirmListener?.invoke(defaultTitle, description)
                } else {
                    onConfirmListener?.invoke(title, description)
                }
            }
            .setNegativeButton("Hủy", null)
            .create()
    }

    fun setOnConfirmListener(listener: (String, String) -> Unit) {
        onConfirmListener = listener
    }
}