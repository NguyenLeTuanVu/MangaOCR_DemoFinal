package com.example.mangaocr_demon.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.mangaocr_demon.data.model.TextBlock
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OcrEngine(private val context: Context) {

    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val chineseRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    private val japaneseRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    private val languageIdentifier = LanguageIdentification.getClient()

    /**
     * Process image and extract text blocks with positions
     */
    suspend fun processImage(imageUri: String): Pair<List<TextBlock>, String> {
        val bitmap = loadBitmap(imageUri) ?: throw Exception("Cannot load image")
        val image = InputImage.fromBitmap(bitmap, 0)

        val imageWidth = bitmap.width
        val imageHeight = bitmap.height

        // Try Chinese first (most common in manga)
        val chineseResult = chineseRecognizer.process(image).await()
        val latinResult = latinRecognizer.process(image).await()
        val japaneseResult = japaneseRecognizer.process(image).await()

        // Combine results
        val allBlocks = mutableListOf<TextBlock>()
        var dominantLanguage = "unknown"
        val languageCounts = mutableMapOf<String, Int>()

        // Process Chinese results
        chineseResult.textBlocks.forEach { block ->
            val bounds = block.boundingBox ?: return@forEach
            val text = block.text

            val language = detectLanguage(text)
            languageCounts[language] = (languageCounts[language] ?: 0) + 1

            allBlocks.add(
                TextBlock(
                    left = bounds.left.toFloat() / imageWidth,
                    top = bounds.top.toFloat() / imageHeight,
                    right = bounds.right.toFloat() / imageWidth,
                    bottom = bounds.bottom.toFloat() / imageHeight,
                    originalText = text,
                    language = language,
                    confidence = 0f
                )
            )
        }

        // Process Latin results (English)
        latinResult.textBlocks.forEach { block ->
            val bounds = block.boundingBox ?: return@forEach
            val text = block.text

            // Check if this block already exists in Chinese results
            if (allBlocks.none { it.originalText == text }) {
                val language = "en"
                languageCounts[language] = (languageCounts[language] ?: 0) + 1

                allBlocks.add(
                    TextBlock(
                        left = bounds.left.toFloat() / imageWidth,
                        top = bounds.top.toFloat() / imageHeight,
                        right = bounds.right.toFloat() / imageWidth,
                        bottom = bounds.bottom.toFloat() / imageHeight,
                        originalText = text,
                        language = language,
                        confidence = 0f
                    )
                )
            }
        }

        // Determine dominant language
        dominantLanguage = languageCounts.maxByOrNull { it.value }?.key ?: "unknown"

        bitmap.recycle()

        return Pair(allBlocks, dominantLanguage)
    }

    private suspend fun detectLanguage(text: String): String = suspendCoroutine { continuation ->
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                val detected = when {
                    languageCode == "und" -> "unknown"
                    languageCode.startsWith("zh") -> "zh"
                    languageCode.startsWith("ja") -> "ja"
                    languageCode.startsWith("en") -> "en"
                    else -> languageCode
                }
                continuation.resume(detected)
            }
            .addOnFailureListener {
                continuation.resume("unknown")
            }
    }

    private fun loadBitmap(imageUri: String): Bitmap? {
        return try {
            val uri = Uri.parse(imageUri)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            android.util.Log.e("OcrEngine", "Error loading bitmap", e)
            null
        }
    }

    fun cleanup() {
        latinRecognizer.close()
        chineseRecognizer.close()
        japaneseRecognizer.close()
        languageIdentifier.close()
    }
}