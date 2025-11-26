package com.example.mangaocr_demon.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mangaocr_demon.R
import com.example.mangaocr_demon.data.AlbumChapterEntity
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.data.ChapterEntity
import com.example.mangaocr_demon.data.MangaEntity
import com.example.mangaocr_demon.databinding.DialogSelectChaptersBinding
import kotlinx.coroutines.launch

class SelectChaptersDialogFragment : DialogFragment() {

    private var _binding: DialogSelectChaptersBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: AppDatabase
    private lateinit var chapterAdapter: SelectableChapterAdapter

    private var albumId: Long = -1
    private var mangaList = listOf<MangaEntity>()
    private var selectedMangaId: Long = -1

    companion object {
        private const val ARG_ALBUM_ID = "album_id"

        fun newInstance(albumId: Long): SelectChaptersDialogFragment {
            return SelectChaptersDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ALBUM_ID, albumId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        albumId = arguments?.getLong(ARG_ALBUM_ID) ?: -1
        setStyle(STYLE_NORMAL, R.style.Theme_MangaOCR_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSelectChaptersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())

        setupRecyclerView()
        loadMangaList()
        setupListeners()
    }

    private fun setupRecyclerView() {
        chapterAdapter = SelectableChapterAdapter()
        binding.rvChapters.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chapterAdapter
        }

        chapterAdapter.onSelectionChanged = { selectedCount ->
            // SỬA Ở ĐÂY
            binding.tvSelectedCount.text = "Đã chọn: $selectedCount chapter"
            binding.btnAddToAlbum.isEnabled = selectedCount > 0
        }
    }

    private fun loadMangaList() {
        lifecycleScope.launch {
            db.mangaDao().getAllManga().collect { list ->
                mangaList = list
                setupMangaSpinner()
            }
        }
    }

    private fun setupMangaSpinner() {
        if (mangaList.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            // SỬA Ở ĐÂY
            binding.tvEmptyState.text = "Chưa có manga nào. Vui lòng thêm manga trước."
            binding.rvChapters.visibility = View.GONE
            return
        }

        val mangaTitles = mangaList.map { it.title }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            mangaTitles
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerManga.adapter = adapter

        binding.spinnerManga.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMangaId = mangaList[position].id
                loadChaptersForManga(selectedMangaId)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadChaptersForManga(mangaId: Long) {
        lifecycleScope.launch {
            db.chapterDao().getChaptersForManga(mangaId).collect { chapters ->
                if (chapters.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    // SỬA Ở ĐÂY
                    binding.tvEmptyState.text = "Manga này chưa có chapter nào"
                    binding.rvChapters.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvChapters.visibility = View.VISIBLE
                    chapterAdapter.submitList(chapters)
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnAddToAlbum.setOnClickListener {
            addSelectedChaptersToAlbum()
        }
    }

    private fun addSelectedChaptersToAlbum() {
        val selectedChapters = chapterAdapter.getSelectedChapters()

        if (selectedChapters.isEmpty()) {
            // SỬA Ở ĐÂY
            Toast.makeText(requireContext(), "Vui lòng chọn ít nhất 1 chapter", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                var addedCount = 0
                var skippedCount = 0

                selectedChapters.forEach { chapter ->
                    // Kiểm tra xem chapter đã có trong album chưa
                    val existing = db.albumChapterDao().isChapterInAlbum(albumId, chapter.id)

                    if (existing == null) {
                        val albumChapter = AlbumChapterEntity(
                            albumId = albumId,
                            chapterId = chapter.id
                        )
                        db.albumChapterDao().addChapterToAlbum(albumChapter)
                        addedCount++
                    } else {
                        skippedCount++
                    }
                }

                // SỬA CÁC CHUỖI MESSAGE Ở ĐÂY
                val message = when {
                    addedCount > 0 && skippedCount > 0 ->
                        "Đã thêm $addedCount chapter. $skippedCount chapter đã tồn tại."
                    addedCount > 0 ->
                        "Đã thêm $addedCount chapter vào album"
                    else ->
                        "Tất cả chapter đã tồn tại trong album"
                }

                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                dismiss()

            } catch (e: Exception) {
                // SỬA Ở ĐÂY
                Toast.makeText(
                    requireContext(),
                    "Lỗi khi thêm chapter: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
