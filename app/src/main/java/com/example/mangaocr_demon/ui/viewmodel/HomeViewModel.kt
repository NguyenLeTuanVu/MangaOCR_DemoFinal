package com.example.mangaocr_demon.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.example.mangaocr_demon.data.*
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _ocrText = MutableLiveData<String>()
    val ocrText: LiveData<String> get() = _ocrText

    private val _translatedText = MutableLiveData<String>()
    val translatedText: LiveData<String> get() = _translatedText

    private val _imageUri = MutableLiveData<Uri?>()
    val imageUri: LiveData<Uri?> get() = _imageUri

    private val db = AppDatabase.getDatabase(application)
    private val historyDao = db.historyDao()

    // ✅ Return manga ID để navigate
    private val _mangaCreated = MutableLiveData<Long>()
    val mangaCreated: LiveData<Long> = _mangaCreated

    // ✅ Return manga ID để navigate


    fun setImageUri(uri: Uri) {
        _imageUri.postValue(uri)
    }

    fun runOCRAndTranslate(image: InputImage, imageUri: Uri?) {
        val recognizer = TextRecognition.getClient(
            JapaneseTextRecognizerOptions.Builder().build()
        )

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text
                _ocrText.postValue(text)

                if (text.isNotEmpty()) {
                    detectLanguageAndTranslate(text, imageUri)
                }
            }
            .addOnFailureListener { e ->
                _ocrText.postValue("OCR lỗi: ${e.message}")
            }
    }

    private fun detectLanguageAndTranslate(text: String, imageUri: Uri?) {
        val languageIdentifier = LanguageIdentification.getClient()

        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { langCode ->
                val sourceLang = when (langCode) {
                    "ja" -> TranslateLanguage.JAPANESE
                    "zh" -> TranslateLanguage.CHINESE
                    "en" -> TranslateLanguage.ENGLISH
                    else -> TranslateLanguage.ENGLISH
                }
                translateText(text, sourceLang, TranslateLanguage.VIETNAMESE, imageUri)
            }
            .addOnFailureListener {
                _translatedText.postValue("Không xác định được ngôn ngữ.")
            }
    }

    private fun translateText(text: String, sourceLang: String, targetLang: String, imageUri: Uri?) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()
        val translator = Translation.getClient(options)

        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translated ->
                        _translatedText.postValue(translated)

                        val history = HistoryEntity(
                            imageUri = imageUri?.toString(),
                            ocrText = text,
                            translatedText = translated,
                            timestamp = System.currentTimeMillis()
                        )
                        viewModelScope.launch(Dispatchers.IO) {
                            historyDao.insert(history)
                        }
                    }
                    .addOnFailureListener { e ->
                        _translatedText.postValue("Dịch lỗi: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                _translatedText.postValue("Không tải được model dịch: ${e.message}")
            }
    }

    // ------------------- Manga WITHOUT Chapters ------------------- //
    private val mangaDao = db.mangaDao()
    private val chapterDao = db.chapterDao()
    private val pageDao = db.pageDao()

    val mangaList: LiveData<List<MangaEntity>> = mangaDao.getAllManga().asLiveData()

    /**
     * ✅ NEW: Thêm chapter mới với ảnh vào manga có sẵn
     */
    fun addChapterWithImages(mangaId: Long, chapterNumber: Int, imageUris: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Tạo chapter mới
                val chapter = ChapterEntity(
                    mangaId = mangaId,
                    number = chapterNumber,
                    title = "Chapter $chapterNumber"
                )
                val chapterId = chapterDao.insert(chapter)
                android.util.Log.d("HomeViewModel", "Created chapter: $chapterId")

                // Thêm pages
                imageUris.forEachIndexed { index, uri ->
                    val page = PageEntity(
                        chapterId = chapterId,
                        pageIndex = index,
                        imageUri = uri,
                        pageType = "IMAGE"
                    )
                    pageDao.insert(page)
                }

                android.util.Log.d("HomeViewModel", "Added ${imageUris.size} images to chapter $chapterNumber")

            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ Error adding chapter", e)
            }
        }
    }

    /**
     * ✅ NEW: Thêm chapter mới với PDF vào manga có sẵn
     */
    fun addChapterWithPdf(mangaId: Long, chapterNumber: Int, pdfUri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val totalPages = getPdfPageCount(pdfUri)

                if (totalPages <= 0) {
                    android.util.Log.w("HomeViewModel", "PDF has no pages")
                    return@launch
                }

                // Tạo chapter mới
                val chapter = ChapterEntity(
                    mangaId = mangaId,
                    number = chapterNumber,
                    title = "Chapter $chapterNumber"
                )
                val chapterId = chapterDao.insert(chapter)

                // Thêm PDF pages
                for (pdfPageIndex in 0 until totalPages) {
                    val pageEntity = PageEntity(
                        chapterId = chapterId,
                        pageIndex = pdfPageIndex,
                        pdfUri = pdfUri,
                        pdfPageNumber = pdfPageIndex,
                        pageType = "PDF"
                    )
                    pageDao.insert(pageEntity)
                }

                android.util.Log.d("HomeViewModel", "✅ Added PDF with $totalPages pages to chapter $chapterNumber")

            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error adding PDF chapter", e)
            }
        }
    }

    /**
     * ✅ NEW: Thêm ảnh trực tiếp vào manga (không tạo chapter)
     * Chapter chỉ là internal structure, user không thấy
     */
    fun addMangaWithImages(manga: MangaEntity, imageUris: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Tạo manga
                val mangaId = mangaDao.insert(manga)
                android.util.Log.d("HomeViewModel", "Created manga: $mangaId")

                // 2. Tạo chapter ẩn (internal only)
                val chapter = ChapterEntity(
                    mangaId = mangaId,
                    number = 1,
                    title = null // No title needed
                )
                val chapterId = chapterDao.insert(chapter)

                // 3. Thêm pages
                imageUris.forEachIndexed { index, uri ->
                    val page = PageEntity(
                        chapterId = chapterId,
                        pageIndex = index,
                        imageUri = uri,
                        pageType = "IMAGE"
                    )
                    pageDao.insert(page)
                }

                android.util.Log.d("HomeViewModel", "Added ${imageUris.size} images")

                // ✅ Return manga ID
                _mangaCreated.postValue(mangaId)

            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ Error adding manga", e)
            }
        }
    }

    private suspend fun getPdfPageCount(pdfUri: String): Int {
        return withContext(Dispatchers.IO) {
            var pageCount = 0
            val uri = Uri.parse(pdfUri)

            try {
                getApplication<Application>().contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                        pageCount = renderer.pageCount
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error reading PDF", e)
                pageCount = 1
            }

            pageCount
        }
    }

    /**
     * ✅ NEW: PDF cũng không có chapter concept
     */
    fun addMangaFromPdf(manga: MangaEntity, pdfUri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val totalPages = getPdfPageCount(pdfUri)

                if (totalPages <= 0) {
                    android.util.Log.w("HomeViewModel", "PDF has no pages")
                    return@launch
                }

                // 1. Tạo manga
                val mangaId = mangaDao.insert(manga)

                // 2. Tạo chapter ẩn
                val chapter = ChapterEntity(
                    mangaId = mangaId,
                    number = 1,
                    title = null
                )
                val chapterId = chapterDao.insert(chapter)

                // 3. Thêm PDF pages
                for (pdfPageIndex in 0 until totalPages) {
                    val pageEntity = PageEntity(
                        chapterId = chapterId,
                        pageIndex = pdfPageIndex,
                        pdfUri = pdfUri,
                        pdfPageNumber = pdfPageIndex,
                        pageType = "PDF"
                    )
                    pageDao.insert(pageEntity)
                }

                android.util.Log.d("HomeViewModel", "✅ Added PDF with $totalPages pages")

                // ✅ Return manga ID
                _mangaCreated.postValue(mangaId)

            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error processing PDF", e)
            }
        }
    }
}