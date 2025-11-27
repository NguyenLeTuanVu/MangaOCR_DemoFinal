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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first // ✅ Thêm import này

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: MangaAdapter

    private var isFabMenuOpen = false

    private var pendingImageUris: List<String>? = null
    private var pendingPdfUri: String? = null
    private var addingToManga: MangaEntity? = null

    // ✅ Track coroutine jobs để cancel khi destroy
    private var openReaderJob: Job? = null
    private var addImagesJob: Job? = null
    private var addPdfJob: Job? = null

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

                    pendingPdfUri = pdfUri.toString()

                    if (addingToManga != null) {
                        addPdfToExistingManga(addingToManga!!, pdfUri.toString())
                    } else {
                        showAddMangaDialog("PDF")
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast("Lỗi: ${e.message}")
                }
            }
        }
    }

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
                        uris.isEmpty() -> showToast("Bạn chưa chọn ảnh nào")
                        uris.size < MIN_IMAGES -> showToast("Vui lòng chọn ít nhất $MIN_IMAGES ảnh")
                        uris.size > MAX_IMAGES -> showToast("Chỉ được chọn tối đa $MAX_IMAGES ảnh")
                        else -> {
                            pendingImageUris = uris

                            if (addingToManga != null) {
                                addImagesToExistingManga(addingToManga!!, uris)
                            } else {
                                showAddMangaDialog("IMAGE")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast("Lỗi: ${e.message}")
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
                // ✅ Click vào manga → Hiện danh sách chapters
                showChapterList(manga)
            },
            onDeleteClick = { manga ->
                showDeleteMangaDialog(manga)
            },
            onLongClick = { manga ->
                // ✅ Long press → Show menu options
                showMangaOptionsMenu(manga)
            }
        )

        binding.recyclerViewManga.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewManga.adapter = adapter

        viewModel.mangaList.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            updateEmptyState(list.isEmpty())
            binding.tvMangaCount.text = "${list.size} truyện"
        }

        // ✅ KHÔNG observe manga created nữa - không auto navigate
        // User sẽ tự click vào manga để xem chapters

        /*
        viewModel.mangaCreated.observe(viewLifecycleOwner) { mangaId ->
            if (mangaId > 0 && addingToManga == null) {
                openMangaReader(mangaId)
            }
        }
        */

        setupFabMenu()

        return binding.root
    }

    private fun openMangaReader(mangaId: Long) {
        // ✅ Cancel job cũ nếu có
        openReaderJob?.cancel()

        openReaderJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                // ✅ Check fragment vẫn attached
                if (!isAdded) return@launch

                val db = AppDatabase.getDatabase(requireContext())

                // ✅ FIX: Dùng first() để lấy 1 lần duy nhất
                val chapterList = db.chapterDao().getChaptersForManga(mangaId).first()

                if (chapterList.isNotEmpty() && isAdded) {
                    val chapterId = chapterList.first().id
                    navigateToReader(chapterId)
                } else {
                    showToast("Manga này chưa có ảnh nào")
                }

            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    android.util.Log.e("HomeFragment", "Error opening manga", e)
                    showToast("Không thể mở: ${e.message}")
                }
            }
        }
    }

    private fun navigateToReader(chapterId: Long) {
        if (!isAdded || parentFragmentManager.isStateSaved) return

        try {
            val readerFragment = ChapterReaderFragment.newInstance(chapterId)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, readerFragment)
                .addToBackStack("reader")
                .commit()
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "Navigation error", e)
        }
    }

    private fun addImagesToExistingManga(manga: MangaEntity, imageUris: List<String>) {
        // ✅ Cancel job cũ
        addImagesJob?.cancel()

        addImagesJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!isAdded) return@launch

                val db = AppDatabase.getDatabase(requireContext())

                // ✅ FIX: Dùng first() thay vì collect để lấy 1 lần rồi xong
                val existingChapters = db.chapterDao().getChaptersForManga(manga.id).first()

                val nextChapterNumber = existingChapters.size + 1

                // ✅ Tạo chapter trong IO thread
                withContext(Dispatchers.IO) {
                    // Tạo chapter mới
                    val chapter = com.example.mangaocr_demon.data.ChapterEntity(
                        mangaId = manga.id,
                        number = nextChapterNumber,
                        title = "Chapter $nextChapterNumber"
                    )
                    val chapterId = db.chapterDao().insert(chapter)

                    // Thêm pages
                    imageUris.forEachIndexed { index, uri ->
                        val page = com.example.mangaocr_demon.data.PageEntity(
                            chapterId = chapterId,
                            pageIndex = index,
                            imageUri = uri,
                            pageType = "IMAGE"
                        )
                        db.pageDao().insert(page)
                    }
                }

                showToast("✅ Đã thêm Chapter $nextChapterNumber với ${imageUris.size} ảnh vào \"${manga.title}\"")

                // Reset state
                addingToManga = null
                pendingImageUris = null

            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    android.util.Log.e("HomeFragment", "Error adding images", e)
                    showToast("Lỗi: ${e.message}")
                }
            }
        }
    }

    private fun addPdfToExistingManga(manga: MangaEntity, pdfUri: String) {
        // ✅ Cancel job cũ
        addPdfJob?.cancel()

        addPdfJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!isAdded) return@launch

                val db = AppDatabase.getDatabase(requireContext())

                // ✅ FIX: Dùng first() thay vì collect
                val existingChapters = db.chapterDao().getChaptersForManga(manga.id).first()

                val nextChapterNumber = existingChapters.size + 1

                withContext(Dispatchers.IO) {
                    // Get PDF page count
                    var totalPages = 0
                    try {
                        requireContext().contentResolver.openFileDescriptor(
                            android.net.Uri.parse(pdfUri), "r"
                        )?.use { pfd ->
                            android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                                totalPages = renderer.pageCount
                            }
                        }
                    } catch (e: Exception) {
                        totalPages = 1
                    }

                    // Tạo chapter
                    val chapter = com.example.mangaocr_demon.data.ChapterEntity(
                        mangaId = manga.id,
                        number = nextChapterNumber,
                        title = "Chapter $nextChapterNumber"
                    )
                    val chapterId = db.chapterDao().insert(chapter)

                    // Thêm PDF pages
                    for (pdfPageIndex in 0 until totalPages) {
                        val pageEntity = com.example.mangaocr_demon.data.PageEntity(
                            chapterId = chapterId,
                            pageIndex = pdfPageIndex,
                            pdfUri = pdfUri,
                            pdfPageNumber = pdfPageIndex,
                            pageType = "PDF"
                        )
                        db.pageDao().insert(pageEntity)
                    }
                }

                showToast("✅ Đã thêm Chapter $nextChapterNumber từ PDF vào \"${manga.title}\"")

                addingToManga = null
                pendingPdfUri = null

            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    android.util.Log.e("HomeFragment", "Error adding PDF", e)
                    showToast("Lỗi: ${e.message}")
                }
            }
        }
    }

    private fun showAddMangaDialog(sourceType: String) {
        if (!isAdded || childFragmentManager.isStateSaved) return

        val dialog = AddMangaDialogFragment.newInstance(sourceType)
        dialog.setOnConfirmListener { title, description ->
            when (sourceType) {
                "PDF" -> {
                    pendingPdfUri?.let { uri ->
                        val manga = MangaEntity(
                            title = title,
                            description = description.ifEmpty { "Nhập từ PDF" },
                            coverImageUri = null
                        )
                        viewModel.addMangaFromPdf(manga, uri)
                        showToast("✅ Đã thêm manga \"$title\" từ PDF")
                        pendingPdfUri = null
                    }
                }
                "IMAGE" -> {
                    pendingImageUris?.let { uris ->
                        val coverUri = if (uris.isNotEmpty()) uris[0] else null

                        val manga = MangaEntity(
                            title = title,
                            description = description.ifEmpty { "${uris.size} ảnh" },
                            coverImageUri = coverUri
                        )
                        viewModel.addMangaWithImages(manga, uris)
                        showToast("✅ Đã thêm manga \"$title\" với ${uris.size} ảnh")
                        pendingImageUris = null
                    }
                }
            }
        }
        dialog.show(childFragmentManager, "AddMangaDialog")
    }

    private fun showDeleteMangaDialog(manga: MangaEntity) {
        if (!isAdded || context == null) return

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Xóa \"${manga.title}\"?")
            .setMessage("Xóa manga này sẽ xóa toàn bộ ảnh bên trong. Hành động không thể hoàn tác.")
            .setPositiveButton("Xóa") { _, _ ->
                deleteManga(manga)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteManga(manga: MangaEntity) {
        if (!isAdded || context == null) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())

                withContext(Dispatchers.IO) {
                    db.mangaDao().delete(manga)
                }

                showToast("Đã xóa \"${manga.title}\"")

            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Error deleting", e)
                showToast("Lỗi: ${e.message}")
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
            addingToManga = null
            openPdfPicker()
        }

        binding.btnAddImages.setOnClickListener {
            closeFabMenu()
            addingToManga = null
            showImagePickerInfo()
        }
    }

    private fun showImagePickerInfo() {
        showToast("Vui lòng chọn từ $MIN_IMAGES đến $MAX_IMAGES ảnh")
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

    private fun showMangaOptionsMenu(manga: MangaEntity) {
        if (!isAdded || context == null) return

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(manga.title)
            .setItems(arrayOf(
                "Mở xem ngay (Chapter đầu tiên)",
                "Thêm ảnh vào manga này",
                "Thêm PDF vào manga này"
            )) { _, which ->
                when (which) {
                    0 -> openMangaReader(manga.id) // ✅ Mở reader trực tiếp
                    1 -> {
                        addingToManga = manga
                        showImagePickerInfo()
                    }
                    2 -> {
                        addingToManga = manga
                        openPdfPicker()
                    }
                }
            }
            .show()
    }

    private fun showChapterList(manga: MangaEntity) {
        if (!isAdded || parentFragmentManager.isStateSaved) return

        val fragment = MangaChapterListFragment.newInstance(manga.id, manga.title)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("chapter_list")
            .commit()
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

    // ✅ Helper function để show toast an toàn
    private fun showToast(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // ✅ Cancel tất cả coroutines
        openReaderJob?.cancel()
        addImagesJob?.cancel()
        addPdfJob?.cancel()

        _binding = null
    }

    companion object {
        private const val MIN_IMAGES = 1
        private const val MAX_IMAGES = 25
    }
}