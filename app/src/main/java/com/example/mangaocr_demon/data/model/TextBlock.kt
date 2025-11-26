package com.example.mangaocr_demon.data.model

import android.graphics.RectF
import kotlinx.serialization.Serializable

@Serializable
data class TextBlock(
    val id: String = java.util.UUID.randomUUID().toString(),

    // Vá»‹ trÃ­ trÃªn áº£nh (normalized 0-1)
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,

    val originalText: String,
    val translatedText: String = "",
    val language: String = "unknown", // "zh", "en", "ja"
    val confidence: Float = 0f,

    // User customization
    val isManuallyEdited: Boolean = false,
    val customFontSize: Float? = null,
    val customBackgroundColor: Int? = null
) {
    // Helper function to get RectF
    fun getBounds(imageWidth: Int, imageHeight: Int): RectF {
        return RectF(
            left * imageWidth,
            top * imageHeight,
            right * imageWidth,
            bottom * imageHeight
        )
    }
}

@Serializable
data class OcrData(
    val textBlocks: List<TextBlock>,
    val imageWidth: Int,
    val imageHeight: Int,
    val processedAt: Long = System.currentTimeMillis()
)