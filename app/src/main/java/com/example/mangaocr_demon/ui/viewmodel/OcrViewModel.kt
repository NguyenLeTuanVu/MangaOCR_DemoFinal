// File: ui/viewmodel/OcrViewModel.kt
package com.example.mangaocr_demon.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.data.HistoryEntity
import com.example.mangaocr_demon.data.PageEntity
import com.example.mangaocr_demon.data.model.OcrData
import com.example.mangaocr_demon.data.model.TextBlock
import com.example.mangaocr_demon.data.repository.HistoryRepository
import com.example.mangaocr_demon.ml.OcrEngine
import com.example.mangaocr_demon.ml.GeminiTranslator // ⭐ Changed from ChatGptTranslator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class OcrViewModel(application: Application) : AndroidViewModel(application) {

    private val ocrEngine = OcrEngine(application)
    private val translator = GeminiTranslator(application) // ⭐ Changed to Gemini

    private val pageDao = AppDatabase.getDatabase(application).pageDao()

    private val historyDao = AppDatabase.getDatabase(application).historyDao() // ⭐ NEW
    private val historyRepository = HistoryRepository(historyDao)

    private val _ocrProgress = MutableLiveData<OcrProgress>()
    val ocrProgress: LiveData<OcrProgress> = _ocrProgress

    private val _translationProgress = MutableLiveData<TranslationProgress>()
    val translationProgress: LiveData<TranslationProgress> = _translationProgress

    private val _ocrError = MutableLiveData<String?>()
    val ocrError: LiveData<String?> = _ocrError

    /**
     * Process OCR for a single page
     */
    fun processPage(page: PageEntity) {
        if (page.imageUri == null) {
            _ocrError.value = "No image URI found"
            return
        }

        viewModelScope.launch {
            try {
                _ocrProgress.value = OcrProgress.Processing

                val result = withContext(Dispatchers.IO) {
                    ocrEngine.processImage(page.imageUri)
                }

                val (textBlocks, dominantLanguage) = result

                if (textBlocks.isEmpty()) {
                    _ocrProgress.value = OcrProgress.NoTextFound
                    return@launch
                }

                val bitmap = withContext(Dispatchers.IO) {
                    android.graphics.BitmapFactory.decodeStream(
                        getApplication<Application>().contentResolver.openInputStream(
                            android.net.Uri.parse(page.imageUri)
                        )
                    )
                }

                val ocrData = OcrData(
                    textBlocks = textBlocks,
                    imageWidth = bitmap?.width ?: 0,
                    imageHeight = bitmap?.height ?: 0
                )
                bitmap?.recycle()

                val jsonData = Json.encodeToString(ocrData)

                withContext(Dispatchers.IO) {
                    pageDao.updateOcrData(
                        pageId = page.id,
                        ocrDataJson = jsonData,
                        isProcessed = true,
                        language = dominantLanguage,
                        timestamp = System.currentTimeMillis()
                    )
                }

                _ocrProgress.value = OcrProgress.Success(textBlocks.size, dominantLanguage)

            } catch (e: Exception) {
                android.util.Log.e("OcrViewModel", "OCR processing failed", e)
                _ocrError.value = "OCR failed: ${e.message}"
                _ocrProgress.value = OcrProgress.Error(e.message ?: "Unknown error")
            }
        }
    }

    // ⭐ Translate page using Gemini
    fun translatePage(page: PageEntity, mangaTitle: String? = null, chapterNumber: String? = null) {
        if (page.ocrDataJson.isNullOrEmpty()) {
            _ocrError.value = "No OCR data found. Please scan the page first."
            return
        }

        viewModelScope.launch {
            try {
                _translationProgress.value = TranslationProgress.Processing

                android.util.Log.d("OcrViewModel", "🔄 Starting translation for page ${page.id}")

                // Parse OCR data
                val ocrData = Json.decodeFromString<OcrData>(page.ocrDataJson)
                android.util.Log.d("OcrViewModel", "📝 Translating ${ocrData.textBlocks.size} blocks")

                // Call API to translate
                val result = translator.translateBlocks(ocrData.textBlocks)

                result.fold(
                    onSuccess = { translatedBlocks ->
                        android.util.Log.d("OcrViewModel", "✅ Translation successful")

                        // Update OCR data with translations
                        val updatedOcrData = ocrData.copy(textBlocks = translatedBlocks)
                        val jsonData = Json.encodeToString(updatedOcrData)

                        // Save to database
                        withContext(Dispatchers.IO) {
                            pageDao.updateOcrData(
                                pageId = page.id,
                                ocrDataJson = jsonData,
                                isProcessed = true,
                                language = ocrData.textBlocks.firstOrNull()?.language ?: "unknown",
                                timestamp = System.currentTimeMillis()
                            )
                        }

                        // ⭐ NEW: Save to history
                        saveToHistory(page, translatedBlocks, mangaTitle, chapterNumber)

                        _translationProgress.value = TranslationProgress.Success(translatedBlocks.size)
                    },
                    onFailure = { error ->
                        android.util.Log.e("OcrViewModel", "❌ Translation failed", error)
                        _translationProgress.value = TranslationProgress.Error(error.message ?: "Unknown error")
                    }
                )

            } catch (e: Exception) {
                android.util.Log.e("OcrViewModel", "Translation error", e)
                _translationProgress.value = TranslationProgress.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun saveToHistory(
        page: PageEntity,
        translatedBlocks: List<TextBlock>,
        mangaTitle: String?,
        chapterNumber: String?
    ) {
        try {
            // Create history entries for each text block
            val historyEntries = translatedBlocks.mapIndexed { index, block ->
                HistoryEntity(
                    imageUri = page.imageUri,
                    ocrText = block.originalText,
                    translatedText = block.translatedText,
                    sourceLanguage = block.language,
                    targetLanguage = "vi",
                    pageId = page.id,
                    mangaTitle = mangaTitle,
                    chapterNumber = chapterNumber,
                    timestamp = System.currentTimeMillis() + index, // Offset để đảm bảo thứ tự
                    translationMethod = "Gemini" // or "ChatGPT"
                )
            }

            // Save all to history
            historyRepository.insertAll(historyEntries)

            android.util.Log.d("OcrViewModel", "✅ Saved ${historyEntries.size} items to history")

        } catch (e: Exception) {
            android.util.Log.e("OcrViewModel", "Failed to save history", e)
            // Don't fail the whole translation if history save fails
        }
    }





    fun clearError() {
        _ocrError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        ocrEngine.cleanup()
        translator.cleanup()
    }

    sealed class OcrProgress {
        object Idle : OcrProgress()
        object Processing : OcrProgress()
        object NoTextFound : OcrProgress()
        data class Success(val textBlockCount: Int, val language: String) : OcrProgress()
        data class Error(val message: String) : OcrProgress()
    }

    sealed class TranslationProgress {
        object Idle : TranslationProgress()
        object Processing : TranslationProgress()
        data class Success(val translatedCount: Int) : TranslationProgress()
        data class Error(val message: String) : TranslationProgress()
    }
}