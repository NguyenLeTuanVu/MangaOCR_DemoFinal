package com.example.mangaocr_demon.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.mangaocr_demon.data.AlbumEntity
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.databinding.DialogCreateAlbumBinding
import kotlinx.coroutines.launch

class CreateAlbumDialogFragment : DialogFragment() {

    private var _binding: DialogCreateAlbumBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: AppDatabase

    companion object {
        fun newInstance(): CreateAlbumDialogFragment {
            return CreateAlbumDialogFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateAlbumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnCreate.setOnClickListener {
            createAlbum()
        }
    }

    private fun createAlbum() {
        val name = binding.etAlbumName.text.toString().trim()
        val description = binding.etAlbumDescription.text.toString().trim()

        if (name.isEmpty()) {
            binding.etAlbumName.error = "Vui lòng nhập tên album!"
            return
        }

        val album = AlbumEntity(
            name = name,
            description = description.ifEmpty { null },
            createdAt = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            db.albumDao().insert(album)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}