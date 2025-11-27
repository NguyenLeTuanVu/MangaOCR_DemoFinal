package com.example.mangaocr_demon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.data.ChapterEntity
import com.example.mangaocr_demon.databinding.FragmentMangaChapterListBinding
import com.example.mangaocr_demon.ui.manga.ChapterAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MangaChapterListFragment : Fragment() {

    private var _binding: FragmentMangaChapterListBinding? = null
    private val binding get() = _binding!!

    private lateinit var chapterAdapter: ChapterAdapter
    private lateinit var db: AppDatabase
    private var mangaId: Long = -1L
    private var mangaTitle: String? = null

    // ✅ Track job để cancel
    private var loadChaptersJob: Job? = null

    companion object {
        private const val ARG_MANGA_ID = "manga_id"
        private const val ARG_MANGA_TITLE = "manga_title"

        fun newInstance(mangaId: Long, mangaTitle: String): MangaChapterListFragment {
            return MangaChapterListFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_MANGA_ID, mangaId)
                    putString(ARG_MANGA_TITLE, mangaTitle)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mangaId = it.getLong(ARG_MANGA_ID, -1L)
            mangaTitle = it.getString(ARG_MANGA_TITLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMangaChapterListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())

        binding.tvMangaTitle.text = mangaTitle ?: "Manga"

        setupRecyclerView()
        loadChapters()
        setupBackButton()
    }

    private fun setupRecyclerView() {
        chapterAdapter = ChapterAdapter { chapter ->
            openChapterReader(chapter)
        }

        binding.rvChapters.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chapterAdapter

            // ✅ Thêm long press listener
            addOnItemTouchListener(object : androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(rv: androidx.recyclerview.widget.RecyclerView, e: android.view.MotionEvent): Boolean {
                    if (e.action == android.view.MotionEvent.ACTION_DOWN) {
                        val child = rv.findChildViewUnder(e.x, e.y)
                        if (child != null) {
                            val position = rv.getChildAdapterPosition(child)
                            if (position != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                                child.setOnLongClickListener {
                                    val chapter = chapterAdapter.currentList[position]
                                    showDeleteChapterDialog(chapter)
                                    true
                                }
                            }
                        }
                    }
                    return false
                }
            })
        }
    }

    private fun loadChapters() {
        // ✅ Cancel job cũ
        loadChaptersJob?.cancel()

        loadChaptersJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                // ✅ Check fragment vẫn attached
                if (!isAdded) return@launch

                db.chapterDao().getChaptersForManga(mangaId).collect { chapters ->
                    // ✅ Check binding vẫn tồn tại
                    if (_binding == null || !isAdded) return@collect

                    if (chapters.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.rvChapters.visibility = View.GONE
                    } else {
                        binding.tvEmptyState.visibility = View.GONE
                        binding.rvChapters.visibility = View.VISIBLE
                        chapterAdapter.submitList(chapters)
                    }

                    binding.tvChapterCount.text = "${chapters.size} chapter(s)"
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    android.util.Log.e("MangaChapterList", "Error loading chapters", e)
                }
            }
        }
    }

    private fun openChapterReader(chapter: ChapterEntity) {
        if (!isAdded || parentFragmentManager.isStateSaved) return

        try {
            val readerFragment = ChapterReaderFragment.newInstance(chapter.id)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, readerFragment)
                .addToBackStack("reader")
                .commit()
        } catch (e: Exception) {
            android.util.Log.e("MangaChapterList", "Navigation error", e)
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            if (isAdded && !parentFragmentManager.isStateSaved) {
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun showDeleteChapterDialog(chapter: ChapterEntity) {
        if (!isAdded || context == null) return

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Xóa Chapter ${chapter.number}?")
            .setMessage("Xóa chapter này sẽ xóa tất cả ảnh bên trong. Hành động không thể hoàn tác.")
            .setPositiveButton("Xóa") { _, _ ->
                deleteChapter(chapter)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteChapter(chapter: ChapterEntity) {
        if (!isAdded || context == null) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    db.chapterDao().delete(chapter)
                }

                if (isAdded && context != null) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Đã xóa Chapter ${chapter.number}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                android.util.Log.e("MangaChapterList", "Error deleting chapter", e)
                if (isAdded && context != null) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Lỗi khi xóa: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // ✅ Cancel coroutine
        loadChaptersJob?.cancel()

        _binding = null
    }
}