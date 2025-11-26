// File: ui/reader/MangaOverlayView.kt
package com.example.mangaocr_demon.ui.reader

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.mangaocr_demon.data.model.TextBlock

class MangaOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var textBlocks = listOf<TextBlock>()
    private var imageWidth = 0
    private var imageHeight = 0
    private var showOverlay = true

    private var showTranslation = true

    fun setShowTranslation(show: Boolean) {
        showTranslation = show
        invalidate()
    }

    fun toggleTranslationMode() {
        showTranslation = !showTranslation
        invalidate()
    }




    private val backgroundPaint = Paint().apply {
        color = Color.WHITE
        alpha = 245
        style = Paint.Style.FILL
    }

    private val shadowPaint = Paint().apply {
        color = Color.BLACK
        alpha = 200
        style = Paint.Style.FILL
        maskFilter = android.graphics.BlurMaskFilter(4f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }


    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 40f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        // ⭐ Thêm shadow cho text
        setShadowLayer(2f, 1f, 1f, Color.WHITE)
    }

    private val borderPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private var debugMode = false
    private var onTextBlockClickListener: ((TextBlock) -> Unit)? = null

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_SOFTWARE, null) // ⭐ Enable software rendering for better text
    }

    private fun getCompatibleTypeface(): Typeface {
        return try {
            // Try to use Noto Sans (supports CJK)
            Typeface.create("noto-sans-cjk", Typeface.NORMAL)
                ?: Typeface.create("sans-serif", Typeface.NORMAL)
        } catch (e: Exception) {
            // Fallback to default sans-serif (usually supports basic CJK)
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
    }


    fun setTextBlocks(blocks: List<TextBlock>, imgWidth: Int, imgHeight: Int) {
        this.textBlocks = blocks
        this.imageWidth = imgWidth
        this.imageHeight = imgHeight
        invalidate()
    }

    fun toggleOverlay() {
        showOverlay = !showOverlay
        invalidate()
    }

    fun setDebugMode(enabled: Boolean) {
        debugMode = enabled
        invalidate()
    }

    fun setOnTextBlockClickListener(listener: (TextBlock) -> Unit) {
        onTextBlockClickListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!showOverlay || textBlocks.isEmpty()) return
        if (imageWidth == 0 || imageHeight == 0) return

        // ⭐ FIX: Calculate scale correctly
        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        // ⭐ DEBUG: Log scale factors
        android.util.Log.d("MangaOverlayView", "View: ${width}x${height}, Image: ${imageWidth}x${imageHeight}")
        android.util.Log.d("MangaOverlayView", "Scale: X=$scaleX, Y=$scaleY")

        textBlocks.forEach { block ->
            drawTextBlock(canvas, block, scaleX, scaleY)
        }
    }

    private fun drawTextBlock(canvas: Canvas, block: TextBlock, scaleX: Float, scaleY: Float) {
        // Get original bounds in pixels
        val bounds = block.getBounds(imageWidth, imageHeight)

        // ⭐ DEBUG: Log original bounds
        android.util.Log.d("MangaOverlayView",
            "Block '${block.originalText}': " +
                    "normalized=(${block.left}, ${block.top}, ${block.right}, ${block.bottom})")
        android.util.Log.d("MangaOverlayView",
            "  pixel bounds=(${bounds.left}, ${bounds.top}, ${bounds.right}, ${bounds.bottom})")

        // Scale to view coordinates
        val scaledBounds = RectF(
            bounds.left * scaleX,
            bounds.top * scaleY,
            bounds.right * scaleX,
            bounds.bottom * scaleY
        )

        // ⭐ DEBUG: Log scaled bounds
        android.util.Log.d("MangaOverlayView",
            "  scaled=(${scaledBounds.left}, ${scaledBounds.top}, ${scaledBounds.right}, ${scaledBounds.bottom})")

        // ⭐ Ensure bounds are within view
        if (scaledBounds.left < 0 || scaledBounds.top < 0 ||
            scaledBounds.right > width || scaledBounds.bottom > height) {
            android.util.Log.w("MangaOverlayView", "⚠️ Bounds out of view!")
        }

        // Draw shadow border (optional)
        val expandedBounds = RectF(
            scaledBounds.left - 2f,
            scaledBounds.top - 2f,
            scaledBounds.right + 2f,
            scaledBounds.bottom + 2f
        )
        canvas.drawRoundRect(expandedBounds, 10f, 10f, shadowPaint)

        // Draw background
        canvas.drawRoundRect(scaledBounds, 8f, 8f, backgroundPaint)

        // Draw border in debug mode
        if (debugMode) {
            canvas.drawRoundRect(scaledBounds, 8f, 8f, borderPaint)
        }

        // Draw text
        val displayText = if (showTranslation && block.translatedText.isNotEmpty()) {
            block.translatedText
        } else {
            block.originalText
        }

        drawFittedText(canvas, displayText, scaledBounds)
    }

    private fun cleanTextForCanvas(text: String): String {
        return text
            // Remove zero-width characters
            .replace("\u200B", "") // Zero-width space
            .replace("\u200C", "") // Zero-width non-joiner
            .replace("\u200D", "") // Zero-width joiner
            .replace("\uFEFF", "") // Zero-width no-break space
            .replace("\u00A0", " ") // Non-breaking space
            // Remove control characters
            .replace(Regex("[\\p{Cc}\\p{Cf}]"), "")
            // Normalize whitespace
            .replace(Regex("\\s+"), " ")
            .trim()
    }





    private fun drawFittedText(canvas: Canvas, text: String, bounds: RectF) {
        if (text.isEmpty()) return

        // ⭐ Reduce padding to give more space for text
        val paddingX = 8f // Reduced from potential larger value
        val paddingY = 6f

        val availableWidth = bounds.width() - (paddingX * 2)
        val availableHeight = bounds.height() - (paddingY * 2)

        if (availableWidth <= 0 || availableHeight <= 0) return

        val cleanedText = cleanTextForCanvas(text)

        // ⭐ Better font size calculation
        var fontSize = calculateInitialFontSize(bounds, cleanedText.length)
        textPaint.textSize = fontSize

        val maxIterations = 15 // Increased iterations
        var iteration = 0

        while (iteration < maxIterations) {
            textPaint.textSize = fontSize

            // Measure text dimensions
            val textWidth = textPaint.measureText(cleanedText)
            val metrics = textPaint.fontMetrics
            val textHeight = metrics.descent - metrics.ascent

            // ⭐ Check if text fits (with multiline consideration)
            val estimatedLines = Math.ceil(textWidth / availableWidth.toDouble()).toInt()
            val totalHeight = estimatedLines * textHeight

            if (textWidth <= availableWidth ||
                (estimatedLines > 1 && totalHeight <= availableHeight)) {
                break
            }

            fontSize -= 1.5f // Smaller decrement for finer control
            if (fontSize < 10f) {
                fontSize = 10f
                break
            }
            iteration++
        }

        textPaint.textSize = fontSize

        // Draw text
        if (textPaint.measureText(cleanedText) > availableWidth) {
            drawMultiLineText(canvas, cleanedText, bounds, availableWidth)
        } else {
            val x = bounds.left + paddingX
            val y = bounds.top + paddingY - textPaint.fontMetrics.ascent
            canvas.drawText(cleanedText, x, y, textPaint)
        }
    }

    private fun calculateInitialFontSize(bounds: RectF, textLength: Int): Float {
        val area = bounds.width() * bounds.height()
        val charArea = area / textLength

        // Heuristic: larger area per character = larger font
        return when {
            charArea > 5000 -> 48f
            charArea > 3000 -> 40f
            charArea > 2000 -> 32f
            charArea > 1000 -> 24f
            charArea > 500 -> 18f
            else -> 14f
        }.coerceAtMost(60f) // Max font size
    }





//    // ⭐ NEW: Check if text can be rendered
//    private fun canRenderText(text: String): Boolean {
//        val width = textPaint.measureText(text)
//        return width > 0 && !width.isNaN() && !width.isInfinite()
//    }
//
//    private fun drawPlaceholder(canvas: Canvas, bounds: RectF) {
//        val placeholderPaint = Paint().apply {
//            color = Color.GRAY
//            textSize = 20f
//            typeface = Typeface.MONOSPACE
//        }
//
//        val text = "[Text]"
//        val x = bounds.left + 8f
//        val y = bounds.centerY()
//
//        canvas.drawText(text, x, y, placeholderPaint)
//    }

    private fun drawMultiLineText(canvas: Canvas, text: String, bounds: RectF, maxWidth: Float) {
        // ⭐ FIX: Detect if text is mixed CJK + Latin
        val hasCJK = isCJKText(text)
        val hasLatin = text.any { it.isLetter() && it.code < 128 }

        // Split logic based on text type
        val segments = if (hasCJK && hasLatin) {
            // Mixed text: split more carefully
            splitMixedText(text)
        } else if (hasCJK) {
            // Pure CJK: can break anywhere but prefer after punctuation
            smartSplitCJK(text)
        } else {
            // Latin: split by spaces
            text.split(" ")
        }

        var currentLine = ""
        var y = bounds.top + 8f - textPaint.fontMetrics.ascent
        val lineHeight = textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent + 4f

        for (segment in segments) {
            val separator = if (hasCJK && !segment.matches(Regex("^[\\p{P}\\p{S}]+$"))) "" else " "
            val testLine = if (currentLine.isEmpty()) {
                segment
            } else {
                currentLine + separator + segment
            }

            val testWidth = textPaint.measureText(testLine)

            if (testWidth > maxWidth && currentLine.isNotEmpty()) {
                // Draw current line
                canvas.drawText(currentLine, bounds.left + 8f, y, textPaint)
                currentLine = segment
                y += lineHeight
                if (y > bounds.bottom - 8f) break
            } else {
                currentLine = testLine
            }
        }

        // Draw last line
        if (currentLine.isNotEmpty() && y <= bounds.bottom - 8f) {
            canvas.drawText(currentLine, bounds.left + 8f, y, textPaint)
        }
    }

    private fun smartSplitCJK(text: String): List<String> {
        val result = mutableListOf<String>()
        var current = ""

        text.forEach { char ->
            current += char
            // Break after punctuation or after 2-3 characters
            if (isPunctuation(char) || current.length >= 3) {
                result.add(current)
                current = ""
            }
        }

        if (current.isNotEmpty()) {
            result.add(current)
        }

        return result
    }


    private fun splitMixedText(text: String): List<String> {
        val result = mutableListOf<String>()
        var current = ""
        var lastWasCJK = false

        text.forEach { char ->
            val isCJK = char.code in 0x4E00..0x9FFF ||
                    char.code in 0x3040..0x309F ||
                    char.code in 0x30A0..0x30FF

            if (char.isWhitespace()) {
                if (current.isNotEmpty()) {
                    result.add(current)
                    current = ""
                }
            } else if (isCJK != lastWasCJK && current.isNotEmpty()) {
                // Transition between CJK and Latin
                result.add(current)
                current = char.toString()
            } else {
                current += char

                // Break CJK after 2-3 chars or punctuation
                if (isCJK && (current.length >= 3 || isPunctuation(char))) {
                    result.add(current)
                    current = ""
                }
            }

            lastWasCJK = isCJK
        }

        if (current.isNotEmpty()) {
            result.add(current)
        }

        return result.filter { it.isNotEmpty() }
    }

    private fun isPunctuation(char: Char): Boolean {
        return char in listOf('。', '，', '、', '；', '：', '？', '！', '.', ',', ';', ':', '?', '!')
    }

    private fun isCJKText(text: String): Boolean {
        val cjkCount = text.count { char ->
            char.code in 0x4E00..0x9FFF ||
                    char.code in 0x3040..0x309F ||
                    char.code in 0x30A0..0x30FF
        }
        return cjkCount > text.length / 2 // More than 50% CJK
    }




    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val clickedBlock = findTextBlockAt(event.x, event.y)
            if (clickedBlock != null) {
                onTextBlockClickListener?.invoke(clickedBlock)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findTextBlockAt(x: Float, y: Float): TextBlock? {
        if (imageWidth == 0 || imageHeight == 0) return null

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        return textBlocks.firstOrNull { block ->
            val bounds = block.getBounds(imageWidth, imageHeight)
            val scaledBounds = RectF(
                bounds.left * scaleX,
                bounds.top * scaleY,
                bounds.right * scaleX,
                bounds.bottom * scaleY
            )
            scaledBounds.contains(x, y)
        }
    }
}