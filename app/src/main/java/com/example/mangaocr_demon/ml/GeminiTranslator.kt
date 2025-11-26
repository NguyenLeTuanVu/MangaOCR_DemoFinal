// File: ml/GeminiTranslator.kt
package com.example.mangaocr_demon.ml

import android.content.Context
import com.example.mangaocr_demon.data.model.TextBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import com.example.mangaocr_demon.BuildConfig

class GeminiTranslator(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // ⚠️ IMPORTANT: Lưu API key trong res/values/strings.xml hoặc BuildConfig
    // Lấy API key tại: https://aistudio.google.com/app/apikey
    private val apiKey: String
        get() = BuildConfig.OPENAI_API_KEY

    // Gemini model - có thể dùng: gemini-1.5-flash (nhanh, free tier cao),
    // gemini-1.5-pro (chính xác hơn), gemini-2.0-flash-exp (mới nhất)
    private val model = "gemini-2.0-flash"

    suspend fun translateBlocks(textBlocks: List<TextBlock>): Result<List<TextBlock>> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("GeminiTranslator", "🔄 Translating ${textBlocks.size} blocks")

                val prompt = buildTranslationPrompt(textBlocks)
                android.util.Log.d("GeminiTranslator", "📝 Prompt: $prompt")

                val response = callGeminiApi(prompt)
                android.util.Log.d("GeminiTranslator", "✅ Response: $response")

                val translations = parseTranslationResponse(response, textBlocks)
                android.util.Log.d("GeminiTranslator", "✅ Parsed ${translations.size} translations")

                Result.success(translations)

            } catch (e: Exception) {
                android.util.Log.e("GeminiTranslator", "❌ Translation failed", e)
                Result.failure(e)
            }
        }
    }

    private fun buildTranslationPrompt(textBlocks: List<TextBlock>): String {
        val textsJson = textBlocks.mapIndexed { index, block ->
            """{"index": $index, "text": "${block.originalText.replace("\"", "\\\"").replace("\n", "\\n")}"}"""
        }.joinToString(",\n")

        return """
You are a professional translator specializing in manga/comic translation from Chinese and English to Vietnamese.

Task: Translate the following text blocks from a manga page to Vietnamese.

Guidelines:
- Maintain the original meaning and tone
- Keep cultural context appropriate for Vietnamese readers
- Preserve any sound effects (FX) in a natural way
- Keep the translation concise to fit in speech bubbles
- Use natural Vietnamese language, not literal translation
- The translation MUST be a single-line string with NO line breaks.
- Absolutely DO NOT insert any newline characters, including actual newlines or \n.


Input (JSON array):
[$textsJson]

Output format (JSON array):
[
  {"index": 0, "translation": "Vietnamese translation here"},
  {"index": 1, "translation": "Vietnamese translation here"},
  ...
]

Respond ONLY with the JSON array, no additional text or markdown.
        """.trimIndent()
    }

    private suspend fun callGeminiApi(prompt: String): String {
        val requestBody = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.3f,
                topK = 40,
                topP = 0.95f,
                maxOutputTokens = 2048
            ),
            safetySettings = listOf(
                SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_NONE"),
                SafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_NONE"),
                SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_NONE"),
                SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_NONE")
            )
        )

        val jsonBody = json.encodeToString(GeminiRequest.serializer(), requestBody)

        // Gemini API endpoint
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            android.util.Log.e("GeminiTranslator", "API Error: ${response.code} - $errorBody")
            throw Exception("API call failed: ${response.code} - $errorBody")
        }

        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        android.util.Log.d("GeminiTranslator", "Raw response: $responseBody")

        val geminiResponse = json.decodeFromString<GeminiResponse>(responseBody)

        // Extract text from response
        val text = geminiResponse.candidates?.firstOrNull()
            ?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("No response from Gemini")

        return text
    }

    private fun parseTranslationResponse(response: String, originalBlocks: List<TextBlock>): List<TextBlock> {
        // Remove markdown code blocks if present
        val cleanResponse = response
            .replace("```json", "")
            .replace("```", "")
            .trim()

        android.util.Log.d("GeminiTranslator", "Parsing: $cleanResponse")

        val translations = json.decodeFromString<List<TranslationItem>>(cleanResponse)

        return originalBlocks.mapIndexed { index, block ->
            val translation = translations.find { it.index == index }?.translation
                ?: block.originalText

            block.copy(translatedText = translation)
        }
    }

    // ========== Data classes for Gemini API ==========

    @Serializable
    private data class GeminiRequest(
        val contents: List<GeminiContent>,
        val generationConfig: GenerationConfig? = null,
        val safetySettings: List<SafetySetting>? = null
    )

    @Serializable
    private data class GeminiContent(
        val parts: List<GeminiPart>,
        val role: String = "user"
    )

    @Serializable
    private data class GeminiPart(
        val text: String
    )

    @Serializable
    private data class GenerationConfig(
        val temperature: Float = 0.7f,
        val topK: Int = 40,
        val topP: Float = 0.95f,
        val maxOutputTokens: Int = 2048,
        val stopSequences: List<String>? = null
    )

    @Serializable
    private data class SafetySetting(
        val category: String,
        val threshold: String
    )

    @Serializable
    private data class GeminiResponse(
        val candidates: List<GeminiCandidate>? = null,
        val promptFeedback: PromptFeedback? = null
    )

    @Serializable
    private data class GeminiCandidate(
        val content: GeminiContent? = null,
        val finishReason: String? = null,
        val index: Int? = null,
        val safetyRatings: List<SafetyRating>? = null
    )

    @Serializable
    private data class PromptFeedback(
        val safetyRatings: List<SafetyRating>? = null
    )

    @Serializable
    private data class SafetyRating(
        val category: String? = null,
        val probability: String? = null
    )

    @Serializable
    private data class TranslationItem(
        val index: Int,
        val translation: String
    )

    fun cleanup() {
        // No cleanup needed for OkHttp
    }
}