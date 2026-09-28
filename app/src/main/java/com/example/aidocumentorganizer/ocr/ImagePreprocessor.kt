package com.example.aidocumentorganizer.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.IOException
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Prepares an image so ML Kit can read it as accurately as possible.
 *
 * ML Kit needs roughly 16-24 px per character. Small, low-resolution, dim or
 * washed-out images (typical for gallery thumbnails, screenshots and quick
 * camera shots) fall well below that, and the recognizer then returns
 * scrambled fragments instead of real words.
 */
object ImagePreprocessor {

    /** Very large photos are downsampled while decoding to avoid OutOfMemory. */
    private const val MAX_DECODE_SIDE = 3000

    /** Images whose long side is below this are upscaled before OCR. */
    private const val GOOD_ENOUGH_LONG_SIDE = 1800

    /** Long side the upscaled image aims for. */
    private const val TARGET_LONG_SIDE = 2400

    /** Never enlarge more than this; beyond it there is nothing left to recover. */
    private const val MAX_UPSCALE = 4f

    /** A decoded, correctly rotated image plus its true (pre-scaling) size. */
    class LoadedImage(
        val bitmap: Bitmap,
        val originalWidth: Int,
        val originalHeight: Int
    )

    // ---------------------------------------------------------------------
    // LOADING
    // ---------------------------------------------------------------------

    /**
     * Decodes [uri], applies the EXIF rotation, and downsamples oversized
     * photos. Works for both content:// (gallery) and file:// (camera) URIs.
     */
    fun load(context: Context, uri: Uri): LoadedImage {

        val resolver = context.contentResolver

        // Pass 1: read only the dimensions.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        val rawWidth = bounds.outWidth
        val rawHeight = bounds.outHeight

        if (rawWidth <= 0 || rawHeight <= 0) {
            throw IOException("Unable to read image dimensions")
        }

        // Pass 2: decode, downsampling only if the image is huge.
        var sampleSize = 1
        while (max(rawWidth, rawHeight) / sampleSize > MAX_DECODE_SIDE) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: throw IOException("Unable to decode image")

        val degrees = resolver.openInputStream(uri)?.use { readExifRotation(it) } ?: 0

        val rotated = rotate(decoded, degrees)

        val swapped = degrees == 90 || degrees == 270

        return LoadedImage(
            bitmap = rotated,
            originalWidth = if (swapped) rawHeight else rawWidth,
            originalHeight = if (swapped) rawWidth else rawHeight
        )
    }

    private fun readExifRotation(stream: InputStream): Int =
        try {
            when (
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            0
        }

    private fun rotate(source: Bitmap, degrees: Int): Bitmap {

        if (degrees == 0) return source

        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }

        val rotated = Bitmap.createBitmap(
            source, 0, 0, source.width, source.height, matrix, true
        )

        if (rotated !== source) source.recycle()

        return rotated
    }

    // ---------------------------------------------------------------------
    // ENHANCING
    // ---------------------------------------------------------------------

    /**
     * Returns a NEW grayscale, contrast-stretched bitmap (sharpened and
     * enlarged first if the source was small). [source] is left untouched
     * and remains the caller's to recycle.
     */
    fun enhance(source: Bitmap): Bitmap {

        var working = source
        var upscaled = false

        val longSide = max(source.width, source.height)

        if (longSide < GOOD_ENOUGH_LONG_SIDE) {

            // Enlarge in steps of at most 2x; one big jump looks blocky.
            var remaining = min(MAX_UPSCALE, TARGET_LONG_SIDE.toFloat() / longSide)

            while (remaining > 1.01f) {

                val step = min(remaining, 2f)

                val next = Bitmap.createScaledBitmap(
                    working,
                    (working.width * step).roundToInt(),
                    (working.height * step).roundToInt(),
                    true
                )

                if (working !== source) working.recycle()

                working = next
                remaining /= step
            }

            upscaled = working !== source
        }

        val result = toEnhancedGray(working, sharpen = upscaled)

        if (working !== source) working.recycle()

        return result
    }

    /**
     * Grayscale -> percentile contrast stretch -> (optional) sharpen.
     * Kept in flat primitive arrays to stay light on memory.
     */
    private fun toEnhancedGray(source: Bitmap, sharpen: Boolean): Bitmap {

        val width = source.width
        val height = source.height
        val total = width * height

        val pixels = IntArray(total)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        var gray = ByteArray(total)
        val histogram = IntArray(256)

        for (i in 0 until total) {

            val p = pixels[i]
            val alpha = p ushr 24
            var r = (p shr 16) and 0xFF
            var g = (p shr 8) and 0xFF
            var b = p and 0xFF

            // Transparent PNG/screenshot areas must read as white paper,
            // not black.
            if (alpha < 255) {
                r = (r * alpha + 255 * (255 - alpha)) / 255
                g = (g * alpha + 255 * (255 - alpha)) / 255
                b = (b * alpha + 255 * (255 - alpha)) / 255
            }

            val y = (r * 299 + g * 587 + b * 114) / 1000

            gray[i] = y.toByte()
            histogram[y]++
        }

        // Stretch so the darkest 1% becomes black and the brightest 1% white.
        val cut = total / 100

        var low = 0
        var running = 0
        while (low < 255) {
            running += histogram[low]
            if (running > cut) break
            low++
        }

        var high = 255
        running = 0
        while (high > 0) {
            running += histogram[high]
            if (running > cut) break
            high--
        }

        // A nearly flat image: stretching would only amplify noise.
        if (high - low < 32) {
            low = 0
            high = 255
        }

        val scale = 255f / (high - low)

        val lut = IntArray(256) {
            ((it - low) * scale).roundToInt().coerceIn(0, 255)
        }

        for (i in 0 until total) {
            gray[i] = lut[gray[i].toInt() and 0xFF].toByte()
        }

        if (sharpen) {
            gray = applySharpen(gray, width, height)
        }

        for (i in 0 until total) {
            val v = gray[i].toInt() and 0xFF
            pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(pixels, 0, width, 0, 0, width, height)

        return output
    }

    /** 3x3 sharpen: 5*center - (up + down + left + right). */
    private fun applySharpen(source: ByteArray, width: Int, height: Int): ByteArray {

        val output = source.copyOf()

        for (y in 1 until height - 1) {

            val row = y * width

            for (x in 1 until width - 1) {

                val i = row + x

                val value =
                    5 * (source[i].toInt() and 0xFF) -
                            (source[i - 1].toInt() and 0xFF) -
                            (source[i + 1].toInt() and 0xFF) -
                            (source[i - width].toInt() and 0xFF) -
                            (source[i + width].toInt() and 0xFF)

                output[i] = value.coerceIn(0, 255).toByte()
            }
        }

        return output
    }
}
