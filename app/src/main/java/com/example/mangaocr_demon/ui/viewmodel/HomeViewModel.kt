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
import java.text.SimpleDateFormat
import kotlinx.coroutines.withContext
import java.util.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // ------------------- OCR + Translate (giá»¯ nguyÃªn) ------------------- //
    private val _ocrText = MutableLiveData<String>()
    val ocrText: LiveData<String> get() = _ocrText

    private val _translatedText = MutableLiveData<String>()
    val translatedText: LiveData<String> get() = _translatedText

    private val _imageUri = MutableLiveData<Uri?>()
    val imageUri: LiveData<Uri?> get() = _imageUri

    private val db = AppDatabase.getDatabase(application)
    private val historyDao = db.historyDao()

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
                _ocrText.postValue("OCR lá»—i: ${e.message}")
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
                _translatedText.postValue("KhÃ´ng xÃ¡c Ä‘á»‹nh Ä‘Æ°á»£c ngÃ´n ngá»¯.")
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
                        _translatedText.postValue("Dá»‹ch lá»—i: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                _translatedText.postValue("KhÃ´ng táº£i Ä‘Æ°á»£c model dá»‹ch: ${e.message}")
            }
    }

    // ------------------- Manga / Chapter / Page ------------------- //
    private val mangaDao = db.mangaDao()
    private val chapterDao = db.chapterDao()
    private val pageDao = db.pageDao()

    val mangaList: LiveData<List<MangaEntity>> = mangaDao.getAllManga().asLiveData()

    /**
     * âœ… FIX: ThÃªm táº¥t cáº£ áº£nh vÃ o 1 CHAPTER DUY NHáº¤T
     * KhÃ´ng tá»± Ä‘á»™ng chia chapters ná»¯a
     */
    fun addMangaWithImages(manga: MangaEntity, imageUris: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Táº¡o manga
                val mangaId = mangaDao.insert(manga)
                android.util.Log.d("HomeViewModel", "âœ… Created manga: $mangaId")

                // 2. Táº¡o 1 CHAPTER DUY NHáº¤T
                val chapter = ChapterEntity(
                    mangaId = mangaId,
                    number = 1,
                    title = "Chapter 1"
                )
                val chapterId = chapterDao.insert(chapter)
                android.util.Log.d("HomeViewModel", "âœ… Created single chapter: $chapterId")

                // 3. ThÃªm Táº¤T Cáº¢ áº£nh vÃ o chapter nÃ y
                imageUris.forEachIndexed { index, uri ->
                    val page = PageEntity(
                        chapterId = chapterId,
                        pageIndex = index,
                        imageUri = uri,
                        pageType = "IMAGE"
                    )
                    pageDao.insert(page)
                }

                android.util.Log.d("HomeViewModel", "âœ… Added ${imageUris.size} images to chapter 1")

            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "âŒ Error adding manga", e)
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
                        android.util.Log.d("HomeViewModel", "PDF has $pageCount pages")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error reading PDF page count", e)
                pageCount = 1
            }

            pageCount
        }
    }

    /**
     * âœ… FIX: PDF cÅ©ng vÃ o 1 CHAPTER DUY NHáº¤T
     * KhÃ´ng auto-split chapters ná»¯a
     */
    fun addMangaFromPdf(manga: MangaEntity, pdfUri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("HomeViewModel", "Starting PDF import: $pdfUri")

                val totalPages = getPdfPageCount(pdfUri)

                if (totalPages <= 0) {
                    android.util.Log.w("HomeViewModel", "PDF has no readable pages")
                    return@launch
                }

                // 1. Táº¡o manga
                val mangaId = mangaDao.insert(manga)
                android.util.Log.d("HomeViewModel", "âœ… Created manga: $mangaId")

                // 2. Táº¡o 1 CHAPTER DUY NHáº¤T
                val chapter = ChapterEntity(
                    mangaId = mangaId,
                    number = 1,
                    title = "Chapter 1"
                )
                val chapterId = chapterDao.insert(chapter)
                android.util.Log.d("HomeViewModel", "âœ… Created single chapter: $chapterId")

                // 3. ThÃªm Táº¤T Cáº¢ PDF pages vÃ o chapter nÃ y
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

                android.util.Log.d("HomeViewModel", "âœ… Added all $totalPages PDF pages to chapter 1")

            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error processing PDF: ${e.message}", e)
            }
        }
    }
}