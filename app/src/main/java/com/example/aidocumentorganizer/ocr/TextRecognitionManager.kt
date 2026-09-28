package com.example.aidocumentorganizer.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

/**
 * What the OCR step hands back to the UI.
 *
 * [warning] is non-null when the result is probably unreliable (image too
 * small, blurry, dark ...) so the user is told instead of silently being
 * shown scrambled text.
 */
data class OcrOutcome(
    val text: String,
    val warning: String? = null
)

class TextRecognitionManager {

    // One recognizer, reused. Creating a client per call leaks native resources.
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /** One OCR run over one version of the image. */
    private class Pass(
        val text: String,
        val trustedChars: Int,
        val totalChars: Int
    ) {
        val trustedRatio: Float
            get() = if (totalChars == 0) 0f else trustedChars / totalChars.toFloat()
    }

    /**
     * Reads the text in [imageUri].
     *
     * 1. OCR the image as it is.
     * 2. If confidence is low, enhance it (upscale / contrast / sharpen) and
     *    OCR again.
     * 3. Keep whichever pass produced more trustworthy text.
     */
    suspend fun recognize(context: Context, imageUri: Uri): OcrOutcome =
        withContext(Dispatchers.Default) {

            val loaded = ImagePreprocessor.load(context, imageUri)

            try {

                var best = runPass(loaded.bitmap)

                val confidentEnough =
                    best.trustedRatio >= CONFIDENT_RATIO &&
                            best.totalChars >= MIN_CONFIDENT_CHARS

                if (!confidentEnough) {

                    val enhanced = ImagePreprocessor.enhance(loaded.bitmap)

                    try {
                        val second = runPass(enhanced)

                        if (second.trustedChars > best.trustedChars) {
                            best = second
                        }
                    } finally {
                        enhanced.recycle()
                    }
                }

                buildOutcome(best, loaded.originalWidth, loaded.originalHeight)

            } finally {
                loaded.bitmap.recycle()
            }
        }

    fun close() {
        recognizer.close()
    }

    // ---------------------------------------------------------------------

    private suspend fun runPass(bitmap: Bitmap): Pass {

        val visionText = recognizer
            .process(InputImage.fromBitmap(bitmap, 0))
            .await()

        val builder = StringBuilder()

        var trustedChars = 0
        var totalChars = 0

        for (block in visionText.textBlocks) {

            val keptLines = ArrayList<String>()

            for (line in block.lines) {

                val text = line.text.trim()
                if (text.isEmpty()) continue

                // 0 means "no confidence reported"; treat as neutral.
                val confidence = line.confidence.takeIf { it > 0f } ?: UNKNOWN_CONFIDENCE

                val chars = text.count { !it.isWhitespace() }

                totalChars += chars
                if (confidence >= TRUSTED_CONFIDENCE) trustedChars += chars

                if (isNoise(text, confidence)) continue

                keptLines += text
            }

            if (keptLines.isNotEmpty()) {
                if (builder.isNotEmpty()) builder.append("\n\n")
                builder.append(keptLines.joinToString("\n"))
            }
        }

        return Pass(builder.toString().trim(), trustedChars, totalChars)
    }

    /** Scrambled fragments like "e", "LW", "eti", "—— ," are dropped. */
    private fun isNoise(text: String, confidence: Float): Boolean =
        confidence < MIN_LINE_CONFIDENCE ||
                text.none { it.isLetterOrDigit() } ||
                (text.length <= 2 && confidence < SHORT_LINE_CONFIDENCE)

    private fun buildOutcome(pass: Pass, width: Int, height: Int): OcrOutcome {

        val tooSmall = max(width, height) < LOW_RES_LONG_SIDE

        val advice =
            "Retake the photo closer to the document, in good light and " +
                    "held steady, or choose a larger, sharper image."

        val sizeNote =
            "This image is only $width x $height px, which is too small for " +
                    "its text to be read reliably. "

        val warning = when {

            pass.text.isBlank() ->
                (if (tooSmall) sizeNote else "No readable text was found. ") + advice

            pass.trustedRatio < WARN_BELOW_RATIO ->
                "Some of this text may be inaccurate. " +
                        (if (tooSmall) sizeNote else "") + advice

            else -> null
        }

        return OcrOutcome(text = pass.text, warning = warning)
    }

    private suspend fun <T> Task<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                if (continuation.isActive) continuation.resume(result)
            }
            addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resumeWithException(exception)
            }
        }

    private companion object {

        /** Neutral value when ML Kit reports no confidence. */
        const val UNKNOWN_CONFIDENCE = 0.8f

        /** A line at or above this counts as trustworthy text. */
        const val TRUSTED_CONFIDENCE = 0.6f

        /** Lines below this are treated as recognition noise and dropped. */
        const val MIN_LINE_CONFIDENCE = 0.4f

        /** Very short lines need higher confidence to be kept. */
        const val SHORT_LINE_CONFIDENCE = 0.75f

        /** Skip the enhanced pass if the first one is at least this good. */
        const val CONFIDENT_RATIO = 0.85f
        const val MIN_CONFIDENT_CHARS = 20

        /** Below this share of trusted text the user gets a warning. */
        const val WARN_BELOW_RATIO = 0.6f

        /** Images smaller than this (long side, px) are flagged as low-res. */
        const val LOW_RES_LONG_SIDE = 1000
    }
}
