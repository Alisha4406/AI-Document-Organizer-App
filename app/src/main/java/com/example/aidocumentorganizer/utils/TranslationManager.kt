package com.example.aidocumentorganizer.utils

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

/**
 * Wraps ML Kit's on-device translation so the rest of the app can
 * translate a document's extracted text (including text pulled
 * from a freshly scanned document) into another language without
 * dealing with model downloads / translator lifecycles directly.
 *
 * Translation runs fully on-device once the language model has
 * been downloaded, so a document's text never has to leave the
 * phone to be translated.
 */
object TranslationManager {

    // A friendly display name for every language ML Kit can
    // translate to/from, in one place so every screen shows the
    // same list.
    val supportedLanguages: List<Pair<String, String>> =
        TranslateLanguage.getAllLanguages()
            .mapNotNull { languageCode ->

                val displayName =
                    displayNameFor(languageCode)

                if (displayName != null) {
                    languageCode to displayName
                } else {
                    null
                }
            }
            .sortedBy { it.second }

    private fun displayNameFor(languageCode: String): String? {

        return try {

            val locale =
                java.util.Locale(languageCode)

            val name =
                locale.getDisplayLanguage(java.util.Locale.ENGLISH)

            if (name.isBlank() || name == languageCode) {
                null
            } else {
                name.replaceFirstChar { it.uppercase() }
            }

        } catch (e: Exception) {

            null
        }
    }

    // Below this confidence, ML Kit's guess is treated as too
    // unreliable to translate from automatically. Short, noisy,
    // or mixed-script OCR text (code, receipts, forms) is exactly
    // the case that produces a confident-looking but wrong guess,
    // so callers should surface this to the user instead of
    // silently translating "from" the wrong language.
    private const val CONFIDENT_THRESHOLD = 0.5f

    /**
     * Result of trying to detect the language of a piece of text.
     *
     * [isConfident] is false when ML Kit's best guess falls below
     * [CONFIDENT_THRESHOLD] - the caller should ask the person to
     * confirm or pick the source language rather than trust it.
     */
    data class DetectionResult(
        val languageCode: String,
        val confidence: Float,
        val isConfident: Boolean
    )

    /**
     * Detects the language of [text] and reports back both the
     * best guess and how confident ML Kit was in it, so the UI
     * can flag an uncertain detection instead of translating from
     * a guess that's likely wrong. Calls [onResult] with null if
     * no supported language could be identified at all.
     */
    fun detectLanguageDetailed(
        text: String,
        onResult: (DetectionResult?) -> Unit
    ) {

        if (text.isBlank()) {
            onResult(null)
            return
        }

        val identifier =
            LanguageIdentification.getClient()

        identifier.identifyPossibleLanguages(text)
            .addOnSuccessListener { identifiedLanguages ->

                val best = identifiedLanguages
                    .filter { it.languageTag != "und" }
                    .maxByOrNull { it.confidence }

                if (
                    best == null ||
                    TranslateLanguage.fromLanguageTag(best.languageTag) == null
                ) {
                    onResult(null)
                } else {
                    onResult(
                        DetectionResult(
                            languageCode = best.languageTag,
                            confidence = best.confidence,
                            isConfident = best.confidence >= CONFIDENT_THRESHOLD
                        )
                    )
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    /**
     * Tries to detect the language of [text]. Falls back to
     * [fallbackLanguageCode] (English by default) if detection
     * is inconclusive, which keeps the caller from having to
     * handle a "could not detect" case on its own.
     *
     * Kept for simple call sites that don't need a confidence
     * signal; prefer [detectLanguageDetailed] wherever a wrong
     * guess would silently produce a bad translation.
     */
    fun detectLanguage(
        text: String,
        fallbackLanguageCode: String = TranslateLanguage.ENGLISH,
        onResult: (String) -> Unit
    ) {

        detectLanguageDetailed(text) { detected ->
            onResult(detected?.languageCode ?: fallbackLanguageCode)
        }
    }

    /**
     * Public lookup so screens can show a friendly name (e.g. for
     * a detected source language) without duplicating the locale
     * lookup logic.
     */
    fun displayNameForLanguageCode(languageCode: String): String =
        displayNameFor(languageCode) ?: languageCode

    /**
     * Translates [text] from [sourceLanguageCode] to
     * [targetLanguageCode], downloading the on-device model
     * first if it isn't already available.
     *
     * [onModelDownloading] is called before a model download
     * starts, so the caller can show progress, since the first
     * translation into a given language can take a moment over
     * a slow connection.
     */
    fun translate(
        text: String,
        sourceLanguageCode: String,
        targetLanguageCode: String,
        onModelDownloading: () -> Unit = {},
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {

        if (text.isBlank()) {
            onSuccess("")
            return
        }

        val options =
            TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguageCode)
                .setTargetLanguage(targetLanguageCode)
                .build()

        val translator: Translator =
            Translation.getClient(options)

        val downloadConditions =
            DownloadConditions.Builder()
                .build()

        onModelDownloading()

        translator.downloadModelIfNeeded(downloadConditions)
            .addOnSuccessListener {

                translator.translate(text)
                    .addOnSuccessListener { translatedText ->

                        onSuccess(translatedText)

                        translator.close()
                    }
                    .addOnFailureListener { exception ->

                        onError(exception)

                        translator.close()
                    }
            }
            .addOnFailureListener { exception ->

                onError(exception)

                translator.close()
            }
    }
}
