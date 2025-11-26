package com.example.mangaocr_demon.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mangaocr_demon.R
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.data.ChapterEntity
import com.example.mangaocr_demon.databinding.FragmentAlbumDetailBinding
import com.example.mangaocr_demon.ChapterReaderFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AlbumDetailFragment : Fragment() {

    private var _binding: FragmentAlbumDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var chapterAdapter: AlbumChapterAdapter
    private lateinit var db: AppDatabase
    private var albumId: Long = -1

    // ✅ Theo dõi các coroutine jobs để hủy khi fragment bị hủy
    private var observeAlbumJob: Job? = null
    private var observeChaptersJob: Job? = null

    companion object {
        private const val ARG_ALBUM_ID = "album_id"

        fun newInstance(albumId: Long): AlbumDetailFragment {
            val fragment = AlbumDetailFragment()
            val args = Bundle()
            args.putLong(ARG_ALBUM_ID, albumId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        albumId = arguments?.getLong(ARG_ALBUM_ID) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())

        setupRecyclerView()
        loadAlbumData()
        observeChapters()
        setupAddButton()
    }

    private fun setupRecyclerView() {
        chapterAdapter = AlbumChapterAdapter(
            onChapterClick = { chapter ->
                openChapterReader(chapter)
            },
            onChapterLongClick = { chapter ->
                showChapterOptions(chapter)
            }
        )

        binding.rvChapters.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chapterAdapter
        }
    }

    private fun loadAlbumData() {
        // ✅ Hủy job cũ nếu có
        observeAlbumJob?.cancel()

        observeAlbumJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                db.albumDao().getAlbumById(albumId).collect { album ->
                    // ✅ Kiểm tra binding trước khi dùng
                    if (_binding != null) {
                        album?.let {
                            binding.tvAlbumName.text = it.name
                            binding.tvAlbumDescription.text = it.description ?: "Không có mô tả"
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AlbumDetailFragment", "Lỗi khi tải dữ liệu album", e)
            }
        }
    }

    private fun observeChapters() {
        // ✅ Hủy job cũ nếu có
        observeChaptersJob?.cancel()

        observeChaptersJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                db.albumDao().getChaptersInAlbum(albumId).collect { chapters ->
                    // ✅ Kiểm tra binding trước khi dùng
                    if (_binding != null && isAdded) {
                        chapterAdapter.submitList(chapters)

                        if (chapters.isEmpty()) {
                            binding.tvEmptyState.visibility = View.VISIBLE
                            binding.rvChapters.visibility = View.GONE
                        } else {
                            binding.tvEmptyState.visibility = View.GONE
                            binding.rvChapters.visibility = View.VISIBLE
                        }

                        binding.tvChapterCount.text = "${chapters.size} chapter"
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AlbumDetailFragment", "Lỗi khi theo dõi các chapter", e)
            }
        }
    }

    private fun setupAddButton() {
        binding.btnAddChapter.setOnClickListener {
            showSelectChaptersDialog()
        }
    }

    private fun showSelectChaptersDialog() {
        // ✅ Kiểm tra fragment vẫn còn attached
        if (!isAdded || childFragmentManager.isStateSaved) {
            return
        }

        val dialog = SelectChaptersDialogFragment.newInstance(albumId)
        dialog.show(childFragmentManager, "select_chapters")
    }

    private fun openChapterReader(chapter: ChapterEntity) {
        // ✅ Kiểm tra fragment vẫn còn attached
        if (!isAdded || parentFragmentManager.isStateSaved) {
            return
        }

        val readerFragment = ChapterReaderFragment.newInstance(chapter.id)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, readerFragment)
            .addToBackStack("chapter_reader")
            .commit()
    }

    private fun showChapterOptions(chapter: ChapterEntity) {
        // ✅ Kiểm tra context vẫn còn
        if (!isAdded || context == null) {
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Xóa chapter?")
            .setMessage("Bạn có muốn xóa chapter này khỏi album?")
            .setPositiveButton("Xóa") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        db.albumChapterDao().removeChapterFromAlbumById(albumId, chapter.id)

                        // ✅ Kiểm tra context trước khi show toast
                        if (isAdded && context != null) {
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Đã xóa chapter khỏi album",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AlbumDetailFragment", "Lỗi khi xóa chapter", e)
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // ✅ Hủy tất cả coroutines
        observeAlbumJob?.cancel()
        observeChaptersJob?.cancel()

        _binding = null
    }
}
