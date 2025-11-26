package com.example.mangaocr_demon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.data.PageEntity
import com.example.mangaocr_demon.data.model.TextBlock
import com.example.mangaocr_demon.databinding.FragmentChapterReaderBinding
import com.example.mangaocr_demon.ui.manga.PageAdapter
import com.example.mangaocr_demon.ui.manga.PageVerticalAdapter
import com.example.mangaocr_demon.ui.viewmodel.ChapterReaderViewModel
import com.example.mangaocr_demon.ui.viewmodel.ChapterReaderViewModelFactory
import com.example.mangaocr_demon.ui.viewmodel.OcrViewModel
import com.example.mangaocr_demon.ui.SettingsManager
import com.example.mangaocr_demon.ui.reader.TextBlockDetailDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChapterReaderFragment : Fragment() {

    private var _binding: FragmentChapterReaderBinding? = null
    private val binding get() = _binding!!
    private lateinit var ocrViewModel: OcrViewModel
    private var currentPages: List<PageEntity> = emptyList()
    private var isFabMenuExpanded = false
    private lateinit var pageAdapter: PageAdapter
    private lateinit var pageVerticalAdapter: PageVerticalAdapter
    private lateinit var viewModel: ChapterReaderViewModel
    private lateinit var settingsManager: SettingsManager
    private var chapterId: Long = -1L
    private var mangaTitle: String? = null
    private var chapterNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            chapterId = it.getLong("chapterId", -1L)
            mangaTitle = it.getString("mangaTitle")
            chapterNumber = it.getString("chapterNumber")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChapterReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settingsManager = SettingsManager(requireContext())

        applyTheme()
        applyReadingMode()
        applyScreenSettings()
        setupViewModel()
        setupOcrViewModel()
        setupBackButton()
        setupOcrControls()
    }

    private fun applyTheme() {
        val bgColor = settingsManager.getThemeBackgroundColor()
        val textColor = settingsManager.getThemeTextColor()

        binding.root.setBackgroundColor(bgColor)
        binding.tvChapterTitle.setTextColor(textColor)
        binding.tvPageIndicator.setTextColor(textColor)

        when (settingsManager.theme) {
            SettingsManager.THEME_LIGHT -> {
                binding.header.setBackgroundColor(0xCCFFFFFF.toInt())
            }
            SettingsManager.THEME_SEPIA -> {
                binding.header.setBackgroundColor(0xCCF4ECD8.toInt())
            }
            else -> {
                binding.header.setBackgroundColor(0x80000000.toInt())
            }
        }
    }

    private fun applyReadingMode() {
        when (settingsManager.readingMode) {
            SettingsManager.MODE_VERTICAL -> setupVerticalMode()
            SettingsManager.MODE_WEBTOON -> setupWebtoonMode()
            else -> setupHorizontalMode()
        }
    }

    private fun setupHorizontalMode() {
        binding.viewPagerPages.visibility = View.VISIBLE
        binding.recyclerViewPages.visibility = View.GONE

        pageAdapter = PageAdapter (
            onPageLongClick = { page ->
                processOcrForPage(page)
            },
            onTextBlockClick = { page, textBlock ->
                showTextBlockDetails(page, textBlock)
            }
        )
        binding.viewPagerPages.apply {
            adapter = pageAdapter
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
        }

        binding.viewPagerPages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePageIndicator(position, pageAdapter.itemCount)
            }
        })
    }

    private fun setupVerticalMode() {
        binding.viewPagerPages.visibility = View.GONE
        binding.recyclerViewPages.visibility = View.VISIBLE

        pageVerticalAdapter = PageVerticalAdapter { page ->
        }

        binding.recyclerViewPages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pageVerticalAdapter
        }
    }

    private fun setupWebtoonMode() {
        setupVerticalMode()
    }

    private fun applyScreenSettings() {
        if (settingsManager.keepScreenOn) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        val brightness = settingsManager.brightness
        if (brightness >= 0) {
            val layoutParams = requireActivity().window.attributes
            layoutParams.screenBrightness = brightness / 100f
            requireActivity().window.attributes = layoutParams
        }
    }

    private fun setupViewModel() {
        val db = AppDatabase.getDatabase(requireContext())
        val factory = ChapterReaderViewModelFactory(
            db.pageDao(),
            db.chapterDao(),
            chapterId
        )
        viewModel = ViewModelProvider(this, factory)[ChapterReaderViewModel::class.java]

        viewModel.chapter.observe(viewLifecycleOwner) { chapter ->
            binding.tvChapterTitle.text = "Chapter ${chapter?.number ?: ""}"
            chapterNumber = chapter?.number?.toString()
        }

        viewModel.pages.observe(viewLifecycleOwner) { pages ->
            currentPages = pages // â­ Store current pages

            when (settingsManager.readingMode) {
                SettingsManager.MODE_VERTICAL, SettingsManager.MODE_WEBTOON -> {
                    pageVerticalAdapter.submitList(pages)
                }
                else -> {
                    pageAdapter.submitList(pages)
                }
            }

            if (pages.isNotEmpty()) {
                updatePageIndicator(0, pages.size)
            }
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            if (settingsManager.brightness >= 0) {
                val layoutParams = requireActivity().window.attributes
                layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                requireActivity().window.attributes = layoutParams
            }
            parentFragmentManager.popBackStack()
        }
    }

    private fun updatePageIndicator(currentPage: Int, totalPages: Int) {
        binding.tvPageIndicator.text = "${currentPage + 1} / $totalPages"
    }
    private fun setupOcrViewModel() {
        ocrViewModel = ViewModelProvider(this)[OcrViewModel::class.java]

        ocrViewModel.ocrProgress.observe(viewLifecycleOwner) { progress ->
            handleOcrProgress(progress)
        }

        ocrViewModel.translationProgress.observe(viewLifecycleOwner) { progress ->
            handleTranslationProgress(progress)
        }

        ocrViewModel.ocrError.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                ocrViewModel.clearError()
            }
        }
    }

    private fun handleTranslationProgress(progress: OcrViewModel.TranslationProgress) {
        when (progress) {
            is OcrViewModel.TranslationProgress.Idle -> {
                binding.cardOcrProgress.visibility = View.GONE
            }

            is OcrViewModel.TranslationProgress.Processing -> {
                binding.cardOcrProgress.visibility = View.VISIBLE
                binding.tvOcrProgress.text = "Translating with Gemini..."
            }

            is OcrViewModel.TranslationProgress.Success -> {
                binding.cardOcrProgress.visibility = View.GONE

                Snackbar.make(
                    binding.root,
                    "Translated ${progress.translatedCount} text blocks",
                    Snackbar.LENGTH_LONG
                ).show()
                forceReloadPages()
            }

            is OcrViewModel.TranslationProgress.Error -> {
                binding.cardOcrProgress.visibility = View.GONE
                showError("Translation failed: ${progress.message}")
            }
        }
    }

    private fun setupOcrControls() {
        // FAB Menu toggle
        binding.fabOcrMenu.setOnClickListener {
            toggleFabMenu()
        }

        // Scan current page
        binding.fabScanPage.setOnClickListener {
            scanCurrentPage()
            toggleFabMenu()
        }

        // Translate page
        binding.fabToggleOverlay.setOnClickListener {
            translateCurrentPage()
            toggleFabMenu()
        }

        // ✅ THÊM: Long click để xóa page
        binding.fabScanPage.setOnLongClickListener {
            clearOcrForCurrentPage()
            toggleFabMenu()
            true
        }

        // ✅ THÊM: Nút xóa page
        //binding.fabDeletePage?.setOnClickListener {
        // deleteCurrentPage()
        //  toggleFabMenu()
        //}
    }
    // ✅ THÊM: Function xóa page hiện tại
    private fun deleteCurrentPage() {
        val currentPosition = when (settingsManager.readingMode) {
            SettingsManager.MODE_VERTICAL, SettingsManager.MODE_WEBTOON -> {
                (binding.recyclerViewPages.layoutManager as? LinearLayoutManager)
                    ?.findFirstVisibleItemPosition() ?: 0
            }
            else -> binding.viewPagerPages.currentItem
        }

        if (currentPosition >= 0 && currentPosition < currentPages.size) {
            val page = currentPages[currentPosition]

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Xóa ảnh này?")
                .setMessage("Bạn có chắc muốn xóa ảnh này? Hành động không thể hoàn tác.")
                .setPositiveButton("Xóa") { _, _ ->
                    lifecycleScope.launch {
                        try {
                            val db = AppDatabase.getDatabase(requireContext())

                            // 1. Xóa page
                            db.pageDao().delete(page)
                            android.util.Log.d("ChapterReaderFragment", "✅ Đã xóa trang ${page.id}")

                            // 2. ✅ Kiểm tra xem chapter còn trang nào không
                            val remainingPages = db.pageDao().getPagesByChapterIdSync(chapterId)

                            if (remainingPages.isEmpty()) {
                                // ✅ Không còn trang nào → Xóa chapter
                                android.util.Log.d("ChapterReaderFragment", "⚠️ Không còn trang, đang xóa chapter $chapterId")

                                val chapter = db.chapterDao().getChapterById(chapterId)
                                chapter.collect { chapterEntity ->
                                    chapterEntity?.let {
                                        db.chapterDao().delete(it)
                                        android.util.Log.d("ChapterReaderFragment", "✅ Đã xóa chapter rỗng ${it.number}")

                                        // ✅ Quay lại màn hình trước
                                        withContext(Dispatchers.Main) {
                                            if (isAdded && context != null) {
                                                android.widget.Toast.makeText(
                                                    requireContext(),
                                                    "Đã xóa ảnh cuối cùng. Chapter đã bị xóa.",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()

                                                // Quay về MangaDetailFragment
                                                parentFragmentManager.popBackStack()
                                            }
                                        }
                                    }
                                }
                            } else {
                                // ✅ Còn trang → Chỉ refresh
                                withContext(Dispatchers.Main) {
                                    Snackbar.make(
                                        binding.root,
                                        "Đã xóa ảnh (${remainingPages.size} ảnh còn lại)",
                                        Snackbar.LENGTH_SHORT
                                    ).show()

                                    forceReloadPages()
                                }
                            }

                        } catch (e: Exception) {
                            android.util.Log.e("ChapterReaderFragment", "❌ Lỗi khi xóa trang", e)
                            withContext(Dispatchers.Main) {
                                showError("Lỗi khi xóa: ${e.message}")
                            }
                        }
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        } else {
            showError("Không có ảnh nào để xóa")
        }
    }


    private fun translateCurrentPage() {
        val currentPosition = when (settingsManager.readingMode) {
            SettingsManager.MODE_VERTICAL, SettingsManager.MODE_WEBTOON -> {
                (binding.recyclerViewPages.layoutManager as? LinearLayoutManager)
                    ?.findFirstVisibleItemPosition() ?: 0
            }
            else -> binding.viewPagerPages.currentItem
        }

        if (currentPosition >= 0 && currentPosition < currentPages.size) {
            val page = currentPages[currentPosition]

            if (!page.isOcrProcessed) {
                showError("Please scan the page first")
                return
            }

            ocrViewModel.translatePage(page, mangaTitle, chapterNumber)
        } else {
            showError("No page selected")
        }
    }

    private fun clearOcrForCurrentPage() {
        val currentPosition = when (settingsManager.readingMode) {
            SettingsManager.MODE_VERTICAL, SettingsManager.MODE_WEBTOON -> {
                (binding.recyclerViewPages.layoutManager as? LinearLayoutManager)
                    ?.findFirstVisibleItemPosition() ?: 0
            }
            else -> binding.viewPagerPages.currentItem
        }

        if (currentPosition >= 0 && currentPosition < currentPages.size) {
            val page = currentPages[currentPosition]

            android.util.Log.d("ChapterReaderFragment", "ðŸ—‘ï¸ Clearing OCR data for page ${page.id}")

            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getDatabase(requireContext())
                    db.pageDao().updateOcrData(
                        pageId = page.id,
                        ocrDataJson = "",
                        isProcessed = false,
                        language = "",
                        timestamp = 0L
                    )

                    android.util.Log.d("ChapterReaderFragment", "âœ… OCR data cleared")

                    Snackbar.make(
                        binding.root,
                        "OCR data cleared. You can scan again.",
                        Snackbar.LENGTH_SHORT
                    ).show()

                    refreshCurrentPage()

                } catch (e: Exception) {
                    android.util.Log.e("ChapterReaderFragment", "âŒ Failed to clear OCR", e)
                    showError("Failed to clear OCR: ${e.message}")
                }
            }
        }
    }

    private fun toggleFabMenu() {
        isFabMenuExpanded = !isFabMenuExpanded

        if (isFabMenuExpanded) {
            binding.layoutOcrActions.visibility = View.VISIBLE
            binding.fabOcrMenu.animate().rotation(45f).setDuration(200).start()
        } else {
            binding.layoutOcrActions.visibility = View.GONE
            binding.fabOcrMenu.animate().rotation(0f).setDuration(200).start()
        }
    }

    private fun scanCurrentPage() {
        val currentPosition = when (settingsManager.readingMode) {
            SettingsManager.MODE_VERTICAL, SettingsManager.MODE_WEBTOON -> {
                (binding.recyclerViewPages.layoutManager as? LinearLayoutManager)
                    ?.findFirstVisibleItemPosition() ?: 0
            }
            else -> binding.viewPagerPages.currentItem
        }

        if (currentPosition >= 0 && currentPosition < currentPages.size) {
            val page = currentPages[currentPosition]
            if (page.isOcrProcessed) {
                android.util.Log.d("ChapterReaderFragment", "ðŸ”„ Re-scanning already processed page")
            }

            processOcrForPage(page)
        } else {
            showError("No page selected")
        }
    }

    private fun toggleOverlayForCurrentPage() {
        Snackbar.make(binding.root, "Overlay toggled", Snackbar.LENGTH_SHORT).show()
    }

    private fun toggleDebugMode() {
        Snackbar.make(binding.root, "Debug mode toggled", Snackbar.LENGTH_SHORT).show()
    }

    private fun processOcrForPage(page: PageEntity) {
        if (page.imageUri == null) {
            showError("No image found")
            return
        }
        android.util.Log.d("ChapterReaderFragment", "ðŸ”„ Processing OCR for page ${page.id}")
        ocrViewModel.processPage(page)
    }

    private fun handleOcrProgress(progress: OcrViewModel.OcrProgress) {
        when (progress) {
            is OcrViewModel.OcrProgress.Idle -> {
                binding.cardOcrProgress.visibility = View.GONE
            }

            is OcrViewModel.OcrProgress.Processing -> {
                binding.cardOcrProgress.visibility = View.VISIBLE
                binding.tvOcrProgress.text = "Processing OCR..."
            }

            is OcrViewModel.OcrProgress.NoTextFound -> {
                binding.cardOcrProgress.visibility = View.GONE
                Snackbar.make(
                    binding.root,
                    "No text found in this page",
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            is OcrViewModel.OcrProgress.Success -> {
                binding.cardOcrProgress.visibility = View.GONE

                val message = "Found ${progress.textBlockCount} text blocks (${progress.language})"
                android.util.Log.d("ChapterReaderFragment", "âœ… OCR Success: $message")

                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                    .setAction("View") {
                        // Scroll to the page
                    }
                    .show()

                android.util.Log.d("ChapterReaderFragment", "ðŸ”„ Triggering force refresh...")
                forceReloadPages()
            }

            is OcrViewModel.OcrProgress.Error -> {
                binding.cardOcrProgress.visibility = View.GONE
                showError("OCR Error: ${progress.message}")
            }
        }
    }

    private fun forceReloadPages() {
        android.util.Log.d("ChapterReaderFragment", "ðŸ”„ Force reloading pages from database")

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())

                val freshPages = db.pageDao().getPagesByChapterIdSync(chapterId)

                android.util.Log.d("ChapterReaderFragment", "ðŸ“„ Loaded ${freshPages.size} fresh pages")

                // Log OCR status of each page
                freshPages.forEachIndexed { index, page ->
                    android.util.Log.d("ChapterReaderFragment",
                        "Page $index (id=${page.id}): isOcrProcessed=${page.isOcrProcessed}, " +
                                "ocrDataJson length=${page.ocrDataJson?.length ?: 0}")
                }

                currentPages = freshPages

                when (settingsManager.readingMode) {
                    SettingsManager.MODE_VERTICAL, SettingsManager.MODE_WEBTOON -> {
                        android.util.Log.d("ChapterReaderFragment", "ðŸ“± Updating vertical adapter")
                        pageVerticalAdapter.submitList(null) // Clear first
                        pageVerticalAdapter.submitList(freshPages)
                        pageVerticalAdapter.notifyDataSetChanged()
                    }
                    else -> {
                        android.util.Log.d("ChapterReaderFragment", "ðŸ“± Updating horizontal adapter")
                        pageAdapter.submitList(null) // Clear first
                        pageAdapter.submitList(freshPages)
                        pageAdapter.notifyDataSetChanged()
                    }
                }

                android.util.Log.d("ChapterReaderFragment", "âœ… Adapter updated")

            } catch (e: Exception) {
                android.util.Log.e("ChapterReaderFragment", "âŒ Error reloading pages", e)
                showError("Failed to reload: ${e.message}")
            }
        }
    }

    private fun refreshCurrentPage() {
        viewModel.pages.value?.let { pages ->
            when (settingsManager.readingMode) {
                SettingsManager.MODE_VERTICAL, SettingsManager.MODE_WEBTOON -> {
                    pageVerticalAdapter.submitList(pages)
                    pageVerticalAdapter.notifyDataSetChanged()
                }
                else -> {
                    pageAdapter.submitList(pages)
                    pageAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun showTextBlockDetails(page: PageEntity, textBlock: TextBlock) {
        val dialog = TextBlockDetailDialog.newInstance(page.id, textBlock)

        dialog.onDismissListener = {
            android.util.Log.d("ChapterReaderFragment", "Dialog dismissed, refreshing view")
            forceReloadPages()
        }

        dialog.show(childFragmentManager, "TextBlockDetailDialog")
    }


    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(android.R.color.holo_red_dark, null))
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val layoutParams = requireActivity().window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        requireActivity().window.attributes = layoutParams

        _binding = null
    }

    companion object {
        fun newInstance(chapterId: Long): ChapterReaderFragment {
            return ChapterReaderFragment().apply {
                arguments = Bundle().apply {
                    putLong("chapterId", chapterId)
                    putString("mangaTitle", mangaTitle)
                }
            }
        }
    }
}