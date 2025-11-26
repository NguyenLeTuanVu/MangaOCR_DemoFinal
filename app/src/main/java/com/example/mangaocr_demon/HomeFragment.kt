package com.example.mangaocr_demon

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.activity.result.contract.ActivityResultContracts
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.data.MangaEntity
import com.example.mangaocr_demon.databinding.FragmentHomeBinding
import com.example.mangaocr_demon.ui.manga.MangaAdapter
import com.example.mangaocr_demon.ui.viewmodel.HomeViewModel
import com.example.mangaocr_demon.ui.dialog.AddMangaDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: MangaAdapter

    private var isFabMenuOpen = false

    // ✅ Lưu tạm URIs và type để dùng sau khi dialog confirm
    private var pendingImageUris: List<String>? = null
    private var pendingPdfUri: String? = null

    // Launcher cho PDF
    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { pdfUri ->
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        pdfUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    // ✅ Lưu URI và hiển thị dialog
                    pendingPdfUri = pdfUri.toString()
                    showAddMangaDialog("PDF")

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Launcher cho Images
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                try {
                    val uris = mutableListOf<String>()

                    val clipData = intent.clipData
                    if (clipData != null) {
                        for (i in 0 until clipData.itemCount) {
                            val uri = clipData.getItemAt(i).uri
                            requireContext().contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            uris.add(uri.toString())
                        }
                    } else {
                        intent.data?.let { uri ->
                            requireContext().contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            uris.add(uri.toString())
                        }
                    }

                    when {
                        uris.isEmpty() -> {
                            Toast.makeText(context, "Bạn chưa chọn ảnh nào", Toast.LENGTH_SHORT).show()
                        }
                        uris.size < MIN_IMAGES -> {
                            Toast.makeText(context, "Vui lòng chọn ít nhất $MIN_IMAGES ảnh", Toast.LENGTH_LONG).show()
                        }
                        uris.size > MAX_IMAGES -> {
                            Toast.makeText(context, "Chỉ được chọn tối đa $MAX_IMAGES ảnh. Bạn đã chọn ${uris.size} ảnh.", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            // ✅ Lưu URIs và hiển thị dialog
                            pendingImageUris = uris
                            showAddMangaDialog("IMAGE")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]

        adapter = MangaAdapter(
            onClick = { manga ->
                android.util.Log.d("HomeFragment", "Opening manga: ${manga.title}")
                val bundle = Bundle().apply {
                    putLong("mangaId", manga.id)
                    putString("mangaTitle", manga.title)
                }
                val fragment = MangaDetailFragment().apply {
                    arguments = bundle
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onDeleteClick = { manga ->
                android.util.Log.d("HomeFragment", "Delete clicked for: ${manga.title}")
                showDeleteMangaDialog(manga)
            }
        )

        binding.recyclerViewManga.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewManga.adapter = adapter

        viewModel.mangaList.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            updateEmptyState(list.isEmpty())
            binding.tvMangaCount.text = "${list.size} truyện"
        }

        setupFabMenu()

        return binding.root
    }

    // ✅ NEW: Hiển thị dialog nhập tên manga
    private fun showAddMangaDialog(sourceType: String) {
        if (!isAdded || childFragmentManager.isStateSaved) return

        val dialog = AddMangaDialogFragment.newInstance(sourceType)
        dialog.setOnConfirmListener { title, description ->
            when (sourceType) {
                "PDF" -> {
                    pendingPdfUri?.let { uri ->
                        val manga = MangaEntity(
                            title = title,
                            description = description.ifEmpty { "Nhập từ file PDF" },
                            coverImageUri = null // ✅ PDF không có cover mặc định
                        )
                        viewModel.addMangaFromPdf(manga, uri)
                        Toast.makeText(context, "Đã thêm PDF: $title", Toast.LENGTH_SHORT).show()
                        pendingPdfUri = null
                    }
                }
                "IMAGE" -> {
                    pendingImageUris?.let { uris ->
                        // ✅ Dùng ảnh đầu tiên làm cover
                        val coverUri = if (uris.isNotEmpty()) uris[0] else null

                        val manga = MangaEntity(
                            title = title,
                            description = description.ifEmpty { "Nhập từ ${uris.size} ảnh" },
                            coverImageUri = coverUri // ✅ Set cover image
                        )
                        viewModel.addMangaWithImages(manga, uris)
                        Toast.makeText(context, "Đã thêm ${uris.size} ảnh: $title", Toast.LENGTH_SHORT).show()
                        pendingImageUris = null
                    }
                }
            }
        }
        dialog.show(childFragmentManager, "AddMangaDialog")
    }

    private fun showDeleteMangaDialog(manga: MangaEntity) {
        if (!isAdded || context == null) return

        android.util.Log.d("HomeFragment", "Showing delete dialog for: ${manga.title}")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Xóa \"${manga.title}\"?")
            .setMessage("Xóa manga này sẽ xóa toàn bộ chapters và ảnh bên trong. Hành động không thể hoàn tác.")
            .setPositiveButton("Xóa") { _, _ ->
                android.util.Log.d("HomeFragment", "User confirmed delete")
                deleteManga(manga)
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                android.util.Log.d("HomeFragment", "User cancelled delete")
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteManga(manga: MangaEntity) {
        if (!isAdded || context == null) return

        android.util.Log.d("HomeFragment", "Starting delete for: ${manga.title}")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())

                withContext(Dispatchers.IO) {
                    android.util.Log.d("HomeFragment", "Deleting manga from database: ${manga.id}")
                    db.mangaDao().delete(manga)
                }

                android.util.Log.d("HomeFragment", "✅ Delete successful")

                if (isAdded && context != null) {
                    Toast.makeText(
                        requireContext(),
                        "Đã xóa \"${manga.title}\"",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "❌ Error deleting manga", e)
                if (isAdded && context != null) {
                    Toast.makeText(
                        requireContext(),
                        "Lỗi khi xóa: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupFabMenu() {
        binding.fabAdd.setOnClickListener {
            if (isFabMenuOpen) {
                closeFabMenu()
            } else {
                openFabMenu()
            }
        }

        binding.scrimOverlay.setOnClickListener {
            closeFabMenu()
        }

        binding.btnAddPdf.setOnClickListener {
            closeFabMenu()
            openPdfPicker()
        }

        binding.btnAddImages.setOnClickListener {
            closeFabMenu()
            showImagePickerInfo()
        }
    }

    private fun showImagePickerInfo() {
        Toast.makeText(
            context,
            "Vui lòng chọn từ $MIN_IMAGES đến $MAX_IMAGES ảnh",
            Toast.LENGTH_SHORT
        ).show()
        openImagePicker()
    }

    private fun openFabMenu() {
        isFabMenuOpen = true
        binding.fabMenuContainer.visibility = View.VISIBLE
        binding.scrimOverlay.visibility = View.VISIBLE

        binding.fabAdd.animate()
            .rotation(45f)
            .setDuration(200)
            .start()
    }

    private fun closeFabMenu() {
        isFabMenuOpen = false
        binding.fabMenuContainer.visibility = View.GONE
        binding.scrimOverlay.visibility = View.GONE

        binding.fabAdd.animate()
            .rotation(0f)
            .setDuration(200)
            .start()
    }

    private fun openPdfPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        pdfPickerLauncher.launch(intent)
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        imagePickerLauncher.launch(intent)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerViewManga.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerViewManga.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val MIN_IMAGES = 1
        private const val MAX_IMAGES = 25
    }
}