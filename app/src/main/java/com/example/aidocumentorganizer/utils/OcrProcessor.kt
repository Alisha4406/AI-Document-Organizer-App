package com.example.aidocumentorganizer.utils

import android.content.Context
import android.net.Uri
import com.example.aidocumentorganizer.ocr.OcrOutcome
import com.example.aidocumentorganizer.ocr.TextRecognitionManager

// One shared recognizer for the whole app.
private val textRecognitionManager by lazy { TextRecognitionManager() }

/**
 * Runs OCR on [imageUri]: preprocessing, recognition, noise filtering and a
 * quality warning. Throws if the image cannot be read.
 */
suspend fun processImageWithOCR(
    context: Context,
    imageUri: Uri
): OcrOutcome = textRecognitionManager.recognize(context, imageUri)
