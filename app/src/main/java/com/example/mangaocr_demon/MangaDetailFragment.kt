package com.example.mangaocr_demon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.ui.manga.ChapterAdapter
import com.example.mangaocr_demon.databinding.FragmentMangaDetailBinding
import com.example.mangaocr_demon.viewmodel.MangaDetailViewModel
import com.example.mangaocr_demon.viewmodel.MangaDetailViewModelFactory

class MangaDetailFragment : Fragment() {

    private var _binding: FragmentMangaDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var chapterAdapter: ChapterAdapter
    private lateinit var viewModel: MangaDetailViewModel

    // DÃ¹ng Long Ä‘á»ƒ khá»›p vá»›i PK kiá»ƒu Long trong DB
    private var mangaId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Láº¥y báº±ng getLong â€” náº¿u báº¡n trÆ°á»›c Ä‘Ã³ dÃ¹ng putInt thÃ¬ sá»­a láº¡i bÃªn gá»i
            mangaId = it.getLong("mangaId", -1L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMangaDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup adapter + RecyclerView
        chapterAdapter = ChapterAdapter { chapter ->
            // Navigate to ChapterReaderFragment
            val readerFragment = ChapterReaderFragment.newInstance(chapter.id)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, readerFragment)
                .addToBackStack("chapter_reader")
                .commit()
        }
        binding.rvChapters.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chapterAdapter
        }

        // Láº¥y instance DB (sá»­ dá»¥ng AppDatabase, khÃ´ng pháº£i MangaDatabase)
        val db = AppDatabase.getDatabase(requireContext())

        // Táº¡o factory vÃ  viewmodel báº±ng ViewModelProvider (khÃ´ng cáº§n delegate)
        val factory = MangaDetailViewModelFactory(
            db.mangaDao(),
            db.chapterDao(),
            mangaId
        )
        viewModel = ViewModelProvider(this, factory).get(MangaDetailViewModel::class.java)

        // Quan sÃ¡t manga (thÃ´ng tin)
        viewModel.manga.observe(viewLifecycleOwner) { manga ->
            binding.tvMangaTitle.text = manga?.title ?: "Manga"
        }

        // Quan sÃ¡t danh sÃ¡ch chapters
        viewModel.chapters.observe(viewLifecycleOwner) { chapters ->
            chapterAdapter.submitList(chapters)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}