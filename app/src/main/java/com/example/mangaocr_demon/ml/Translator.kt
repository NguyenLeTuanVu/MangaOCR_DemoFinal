// File: ml/Translator.kt
package com.example.mangaocr_demon.ml

import com.example.mangaocr_demon.data.model.TextBlock
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

class Translator {

    private val translators = mutableMapOf<String, com.google.mlkit.nl.translate.Translator>()

    /**
     * Translate text blocks to Vietnamese
     */
    suspend fun translateBlocks(textBlocks: List<TextBlock>): List<TextBlock> {
        val results = mutableListOf<TextBlock>()

        // Group by language for batch processing
        val groupedByLanguage = textBlocks.groupBy { it.language }

        for ((language, blocks) in groupedByLanguage) {
            val translator = getTranslator(language) ?: continue

            // Ensure model is downloaded
            ensureModelDownloaded(translator)

            // Translate each block
            for (block in blocks) {
                try {
                    val translated = translator.translate(block.originalText).await()
                    results.add(block.copy(translatedText = translated))
                } catch (e: Exception) {
                    android.util.Log.e("Translator", "Translation failed for ${block.originalText}", e)
                    results.add(block) // Keep original if translation fails
                }
            }
        }

        return results
    }

    private fun getTranslator(language: String): com.google.mlkit.nl.translate.Translator? {
        val sourceLanguage = when (language) {
            "zh" -> TranslateLanguage.CHINESE
            "en" -> TranslateLanguage.ENGLISH
            "ja" -> TranslateLanguage.JAPANESE
            else -> return null
        }

        // Check if Vietnamese is supported
        // Note: ML Kit Translation might not support Vietnamese directly
        // You may need to use Google Cloud Translation API instead

        val targetLanguage = TranslateLanguage.VIETNAMESE // ⚠️ Check availability

        val key = "$sourceLanguage-$targetLanguage"

        return translators.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build()
            Translation.getClient(options)
        }
    }

    private suspend fun ensureModelDownloaded(translator: com.google.mlkit.nl.translate.Translator) {
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        translator.downloadModelIfNeeded(conditions).await()
    }

    fun cleanup() {
        translators.values.forEach { it.close() }
        translators.clear()
    }
}