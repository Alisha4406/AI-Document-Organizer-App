package com.example.aidocumentorganizer.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import java.io.File
import java.io.FileOutputStream

/**
 * Builds a simple A4 PDF from a saved document:
 * page 1 = title + scanned image, following pages = the (edited) OCR text.
 * Uses only the framework PdfDocument, so it needs no extra dependency.
 */
object PdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40

    fun export(
        context: Context,
        title: String,
        imagePath: String,
        text: String
    ): File? {

        return try {

            // Multi-page scans store image paths separated by |. The current
            // PDF format uses the first page as the visual cover and keeps
            // the combined OCR text on following pages.
            val primaryImagePath = imagePath.split("|").firstOrNull { it.isNotBlank() }.orEmpty()

            val contentWidth = PAGE_WIDTH - MARGIN * 2
            val pdf = PdfDocument()

            var pageNumber = 1
            var page = pdf.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            var canvas = page.canvas

            fun nextPage() {
                pdf.finishPage(page)
                pageNumber++
                page = pdf.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                )
                canvas = page.canvas
            }

            // ---- Title -------------------------------------------------
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 20f
                isFakeBoldText = true
                color = Color.BLACK
            }

            val safeTitle = TextUtils.ellipsize(
                title, TextPaint(titlePaint), contentWidth.toFloat(), TextUtils.TruncateAt.END
            ).toString()

            var y = MARGIN + 20f
            canvas.drawText(safeTitle, MARGIN.toFloat(), y, titlePaint)
            y += 20f

            // ---- Image -------------------------------------------------
            val bitmap = decodeBitmap(primaryImagePath)

            if (bitmap != null) {

                val maxWidth = contentWidth.toFloat()
                val maxHeight = PAGE_HEIGHT - y - MARGIN

                val scale = minOf(
                    maxWidth / bitmap.width,
                    maxHeight / bitmap.height
                )

                val drawWidth = bitmap.width * scale
                val drawHeight = bitmap.height * scale

                val left = MARGIN + (maxWidth - drawWidth) / 2f

                canvas.drawBitmap(
                    bitmap,
                    null,
                    RectF(left, y, left + drawWidth, y + drawHeight),
                    Paint(Paint.FILTER_BITMAP_FLAG)
                )

                bitmap.recycle()

                // Text goes on its own pages after the image page.
                if (text.isNotBlank()) {
                    nextPage()
                    y = MARGIN.toFloat()
                }
            }

            // ---- Text --------------------------------------------------
            if (text.isNotBlank()) {

                val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 12f
                    color = Color.BLACK
                }

                val layout = StaticLayout.Builder
                    .obtain(text, 0, text.length, textPaint, contentWidth)
                    .setLineSpacing(3f, 1f)
                    .build()

                if (bitmap == null) y += 16f

                for (i in 0 until layout.lineCount) {

                    val lineHeight = layout.getLineBottom(i) - layout.getLineTop(i)

                    if (y + lineHeight > PAGE_HEIGHT - MARGIN) {
                        nextPage()
                        y = MARGIN.toFloat()
                    }

                    val baseline = y + (layout.getLineBaseline(i) - layout.getLineTop(i))

                    canvas.drawText(
                        text,
                        layout.getLineStart(i),
                        layout.getLineVisibleEnd(i),
                        MARGIN.toFloat(),
                        baseline,
                        textPaint
                    )

                    y += lineHeight
                }
            }

            pdf.finishPage(page)

            val directory = File(context.cacheDir, "exports").apply { mkdirs() }

            val safeName = title
                .replace(Regex("[^A-Za-z0-9 _-]"), "")
                .trim()
                .ifBlank { "document" }
                .replace(' ', '_')

            val file = File(directory, "$safeName.pdf")

            FileOutputStream(file).use { pdf.writeTo(it) }
            pdf.close()

            file

        } catch (e: Exception) {
            null
        }
    }

    // Down-samples large camera photos and honours EXIF rotation.
    private fun decodeBitmap(path: String, maxDimension: Int = 1600): Bitmap? {

        if (path.isBlank()) return null

        val file = File(path)
        if (!file.exists()) return null

        return try {

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)

            var sample = 1
            while (
                bounds.outWidth / sample > maxDimension ||
                bounds.outHeight / sample > maxDimension
            ) {
                sample *= 2
            }

            val decoded = BitmapFactory.decodeFile(
                path,
                BitmapFactory.Options().apply { inSampleSize = sample }
            ) ?: return null

            val orientation = ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (degrees == 0f) {
                decoded
            } else {
                Bitmap.createBitmap(
                    decoded, 0, 0, decoded.width, decoded.height,
                    Matrix().apply { postRotate(degrees) }, true
                )
            }

        } catch (e: Exception) {
            null
        }
    }
}
