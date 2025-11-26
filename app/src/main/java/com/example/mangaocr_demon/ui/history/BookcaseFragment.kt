package com.example.mangaocr_demon.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mangaocr_demon.R
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.databinding.FragmentBookcaseBinding
import kotlinx.coroutines.launch
import com.example.mangaocr_demon.data.AlbumEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookcaseFragment : Fragment() {

    private var _binding: FragmentBookcaseBinding? = null
    private val binding get() = _binding!!

    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var db: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookcaseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())

        setupRecyclerView()
        observeAlbums()

        binding.fabAddAlbum.setOnClickListener {
            showCreateAlbumDialog()
        }
    }

    private fun setupRecyclerView() {
        albumAdapter = AlbumAdapter(
            onAlbumClick = { album ->
                // Mở chi tiết album
                val fragment = AlbumDetailFragment.newInstance(album.id)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack("album_detail")
                    .commit()
            },
            onAlbumLongClick = { album ->
                // Long click - có thể dùng cho edit
                showAlbumOptionsDialog(album)
            },
            onDeleteClick = { album ->
                // ✅ NEW: Click nút xóa
                showDeleteAlbumDialog(album)
            }
        )

        binding.rvAlbums.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = albumAdapter
        }
    }

    private fun observeAlbums() {
        lifecycleScope.launch {
            db.albumDao().getAllAlbums().collect { albums ->
                albumAdapter.submitList(albums)

                if (albums.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvAlbums.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvAlbums.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showCreateAlbumDialog() {
        if (!isAdded || childFragmentManager.isStateSaved) return

        val dialog = CreateAlbumDialogFragment.newInstance()
        dialog.show(childFragmentManager, "create_album")
    }

    /**
     * ✅ NEW: Hiển thị dialog xác nhận xóa album
     */
    private fun showDeleteAlbumDialog(album: AlbumEntity) {
        if (!isAdded || context == null) return

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Xóa \"${album.name}\"?")
            .setMessage("Bạn có chắc muốn xóa album này? Các manga trong album sẽ không bị xóa.")
            .setPositiveButton("Xóa") { _, _ ->
                deleteAlbum(album)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    /**
     * ✅ NEW: Xóa album
     */
    private fun deleteAlbum(album: AlbumEntity) {
        if (!isAdded || context == null) return

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Xóa album (cascade sẽ tự động xóa các AlbumMangaEntity liên quan)
                    db.albumDao().delete(album)
                }

                if (isAdded && context != null) {
                    Toast.makeText(
                        requireContext(),
                        "Đã xóa album \"${album.name}\"",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                android.util.Log.e("BookcaseFragment", "Error deleting album", e)
                if (isAdded && context != null) {
                    Toast.makeText(
                        requireContext(),
                        "Lỗi khi xóa: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Options dialog (có thể dùng cho edit album trong tương lai)
     */
    private fun showAlbumOptionsDialog(album: AlbumEntity) {
        if (!isAdded || context == null) return

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(album.name)
            .setItems(arrayOf("Xóa album")) { _, which ->
                when (which) {
                    0 -> showDeleteAlbumDialog(album)
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}