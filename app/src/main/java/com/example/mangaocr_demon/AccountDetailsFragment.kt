package com.example.mangaocr_demon

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.mangaocr_demon.databinding.FragmentAccountDetailsBinding
import com.example.mangaocr_demon.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountDetailsFragment : Fragment() {

    private var _binding: FragmentAccountDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        loadUserInfo()
        loadStatistics()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadUserInfo() {
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        val name = prefs.getString("user_name", "Người dùng") ?: "Người dùng"
        val email = prefs.getString("user_email", "") ?: ""
        val photoUrl = prefs.getString("user_photo", null)

        binding.tvUserName.text = name
        binding.tvUserEmail.text = email

        if (photoUrl != null) {
            Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .placeholder(R.mipmap.ic_launcher)
                .into(binding.ivUserAvatar)
        }
    }

    private fun loadStatistics() {
        // Sử dụng lifecycleScope thay vì CoroutineScope
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(requireContext())

                // Đếm số manga
                var mangaCount = 0
                var totalChapters = 0
                var totalPages = 0
                var historyCount = 0

                // Lấy tất cả manga (chỉ collect 1 lần)
                db.mangaDao().getAllManga().collect { mangas ->
                    mangaCount = mangas.size

                    // Đếm chapters và pages
                    for (manga in mangas) {
                        db.chapterDao().getChaptersForManga(manga.id).collect { chapters ->
                            totalChapters += chapters.size

                            for (chapter in chapters) {
                                db.pageDao().getPagesForChapter(chapter.id).collect { pages ->
                                    totalPages += pages.size
                                }
                            }
                        }
                    }

                    // Đếm lịch sử (sử dụng suspend function thay vì LiveData)
                    // Nếu bạn có suspend function trong HistoryDao
                    // historyCount = db.historyDao().getHistoryCount()

                    // Cập nhật UI trên Main thread
                    withContext(Dispatchers.Main) {
                        updateStatisticsUI(mangaCount, totalChapters, totalPages, historyCount)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    updateStatisticsUI(0, 0, 0, 0)
                }
            }
        }
    }

    private fun updateStatisticsUI(mangaCount: Int, chapterCount: Int, pageCount: Int, historyCount: Int) {
        binding.tvMangaCount.text = mangaCount.toString()
        binding.tvChapterCount.text = chapterCount.toString()
        binding.tvPageCount.text = pageCount.toString()
        binding.tvHistoryCount.text = historyCount.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): AccountDetailsFragment {
            return AccountDetailsFragment()
        }
    }
}