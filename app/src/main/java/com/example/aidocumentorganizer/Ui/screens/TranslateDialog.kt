package com.example.aidocumentorganizer.Ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aidocumentorganizer.utils.TranslationManager
import com.google.mlkit.nl.translate.TranslateLanguage

// A null source-language code means "auto-detect" - kept distinct
// from an actual language code so the dropdown can show a clear
// "Auto-detect" entry alongside the real language list.
private val AUTO_DETECT: String? = null

/**
 * Lets the user translate [sourceText] into a language of their
 * choice. Works the same whether [sourceText] came from a saved
 * document or straight out of OCR on a freshly scanned image,
 * since ML Kit translation only cares about the text itself.
 *
 * The source language is auto-detected by default, but the person
 * can always override it - important because OCR text (especially
 * short, noisy, or mixed-script text) can fool language detection,
 * which otherwise silently translates from the wrong language and
 * produces garbled output.
 *
 * [onUseTranslation], when provided, shows a button that hands
 * the translated text (and the language it was translated into)
 * back to the caller - e.g. to replace the document text about
 * to be saved, or to save a translated copy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateDialog(
    sourceText: String,
    onDismiss: () -> Unit,
    onUseTranslation: ((translatedText: String, languageName: String) -> Unit)? = null
) {

    var sourceExpanded by remember { mutableStateOf(false) }
    var targetExpanded by remember { mutableStateOf(false) }

    // null = auto-detect (the default and recommended mode).
    var chosenSourceLanguageCode by remember {
        mutableStateOf<String?>(AUTO_DETECT)
    }

    var targetLanguageCode by remember {
        mutableStateOf(TranslateLanguage.SPANISH)
    }

    var detection by remember {
        mutableStateOf<TranslationManager.DetectionResult?>(null)
    }
    var detectionRan by remember { mutableStateOf(false) }

    var translatedText by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val languages = TranslationManager.supportedLanguages

    fun nameFor(code: String): String =
        languages.firstOrNull { it.first == code }?.second
            ?: TranslationManager.displayNameForLanguageCode(code)

    val selectedTargetName = nameFor(targetLanguageCode)

    // The language actually used as the translation source: the
    // person's explicit choice if they made one, otherwise the
    // auto-detected language, otherwise English as a last resort.
    val effectiveSourceCode =
        chosenSourceLanguageCode
            ?: detection?.languageCode
            ?: TranslateLanguage.ENGLISH

    val effectiveSourceName = nameFor(effectiveSourceCode)

    val showLowConfidenceWarning =
        chosenSourceLanguageCode == AUTO_DETECT &&
                detectionRan &&
                detection?.isConfident == false

    fun runTranslation() {

        translatedText = null
        errorMessage = null
        isTranslating = true

        TranslationManager.translate(
            text = sourceText,
            sourceLanguageCode = effectiveSourceCode,
            targetLanguageCode = targetLanguageCode,
            onSuccess = { result ->
                translatedText = result
                isTranslating = false
            },
            onError = {
                errorMessage =
                    "Couldn't translate this document. Check your connection and try again."
                isTranslating = false
            }
        )
    }

    // Detect the source language once, up front.
    LaunchedEffect(sourceText) {
        TranslationManager.detectLanguageDetailed(sourceText) { result ->
            detection = result
            detectionRan = true
            runTranslation()
        }
    }

    // Re-translate whenever the person changes either language,
    // but not before the initial detection above has run.
    LaunchedEffect(chosenSourceLanguageCode, targetLanguageCode) {
        if (detectionRan) {
            runTranslation()
        }
    }

    AlertDialog(

        onDismissRequest = onDismiss,

        icon = {
            Icon(
                imageVector = Icons.Filled.Translate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },

        title = {
            Text(
                text = "Translate Document",
                style = MaterialTheme.typography.titleLarge
            )
        },

        text = {

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // ================================================
                // DETECTED / CHOSEN SOURCE LANGUAGE
                // ================================================

                LanguagePickerField(
                    label = "Translate from",
                    expanded = sourceExpanded,
                    onExpandedChange = { sourceExpanded = it },
                    displayValue = if (chosenSourceLanguageCode == AUTO_DETECT) {
                        if (detectionRan) {
                            "Auto-detected: $effectiveSourceName"
                        } else {
                            "Detecting…"
                        }
                    } else {
                        effectiveSourceName
                    }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (detectionRan) {
                                    "Auto-detect (currently: $effectiveSourceName)"
                                } else {
                                    "Auto-detect"
                                }
                            )
                        },
                        onClick = {
                            chosenSourceLanguageCode = AUTO_DETECT
                            sourceExpanded = false
                        }
                    )

                    languages.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                chosenSourceLanguageCode = code
                                sourceExpanded = false
                            }
                        )
                    }
                }

                // Low-confidence warning: OCR text is often short,
                // noisy, or mixed-script, which can fool automatic
                // detection and silently produce a garbled
                // translation. Surface that instead of hiding it.
                if (showLowConfidenceWarning) {

                    InfoBanner(
                        icon = Icons.Filled.WarningAmber,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        text = "We're not confident about the source language " +
                                "(best guess: $effectiveSourceName). If the " +
                                "translation below looks wrong, pick the correct " +
                                "language above."
                    )
                } else if (chosenSourceLanguageCode == AUTO_DETECT && detectionRan) {

                    InfoBanner(
                        icon = Icons.Filled.Info,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        text = "Detected automatically. Tap above to change it " +
                                "if that's not right."
                    )
                }

                // ================================================
                // TARGET LANGUAGE
                // ================================================

                LanguagePickerField(
                    label = "Translate to",
                    expanded = targetExpanded,
                    onExpandedChange = { targetExpanded = it },
                    displayValue = selectedTargetName
                ) {
                    languages.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                targetLanguageCode = code
                                targetExpanded = false
                            }
                        )
                    }
                }

                // ================================================
                // RESULT
                // ================================================

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp, max = 260.dp)
                            .padding(14.dp)
                    ) {

                        when {

                            isTranslating -> {

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {

                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 3.dp
                                    )

                                    Text(
                                        text = "Translating…",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }

                            errorMessage != null -> {

                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            translatedText != null -> {

                                Text(
                                    text = if (translatedText!!.isBlank()) {
                                        "There's no text to translate."
                                    } else {
                                        translatedText!!
                                    },
                                    modifier = Modifier.verticalScroll(
                                        rememberScrollState()
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        },

        confirmButton = {

            if (
                onUseTranslation != null &&
                !translatedText.isNullOrBlank()
            ) {

                TextButton(
                    onClick = {

                        onUseTranslation(
                            translatedText ?: "",
                            selectedTargetName
                        )

                        onDismiss()
                    }
                ) {
                    Text("Use Translation")
                }
            }
        },

        dismissButton = {

            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * A read-only dropdown field styled consistently for both the
 * source- and target-language pickers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerField(
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    displayValue: String,
    menuContent: @Composable () -> Unit
) {

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {

        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            menuContent()
        }
    }
}

@Composable
private fun InfoBanner(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    text: String
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {

        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp
            )
        }
    }
}
