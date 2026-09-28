package com.example.aidocumentorganizer.Ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DocumentTextScreen(
    extractedText: String,
    isProcessing: Boolean,
    errorMessage: String?,
    // Shown above the text when the image was too small/blurry to read
    // reliably, so the user knows the text may be inaccurate.
    warningMessage: String? = null,
    onSaveDocumentClick: () -> Unit,
    onBack: () -> Unit,
    // Lets the scanned text be replaced with a translated version
    // before the document is saved.
    onTextTranslated: (String) -> Unit = {}
) {

    val scrollState = rememberScrollState()

    var showTranslateDialog by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Keep the title clear of the status bar / camera cutout and the
            // buttons clear of the gesture bar so they receive taps.
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Document Text",
            style = MaterialTheme.typography.headlineSmall
        )

        if (isProcessing) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),

                horizontalAlignment = Alignment.CenterHorizontally,

                verticalArrangement = Arrangement.Center
            ) {

                CircularProgressIndicator()

                Text(
                    text = "Processing document...",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Extracting text from your document.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Please wait...",
                    style = MaterialTheme.typography.bodySmall
                )
            }

        } else if (errorMessage != null) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),

                horizontalAlignment = Alignment.CenterHorizontally,

                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "⚠️",
                    style = MaterialTheme.typography.displaySmall
                )

                Text(
                    text = "Unable to process document",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

        } else {

            if (warningMessage != null) {

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {

                    Text(
                        text = "⚠️  $warningMessage",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {

                Text(
                    text = if (extractedText.isBlank()) {
                        "No text was extracted from this document."
                    } else {
                        extractedText
                    },

                    style = MaterialTheme.typography.bodyLarge
                )
            }

            OutlinedButton(
                onClick = {
                    showTranslateDialog = true
                },

                modifier = Modifier.fillMaxWidth(),

                enabled = extractedText.isNotBlank()
            ) {

                Text(
                    text = "Translate"
                )
            }

            Button(
                onClick = onSaveDocumentClick,

                modifier = Modifier.fillMaxWidth(),

                enabled = extractedText.isNotBlank()
            ) {

                Text(
                    text = "Save Document"
                )
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = "Back"
            )
        }
    }

    if (showTranslateDialog) {

        TranslateDialog(
            sourceText = extractedText,
            onDismiss = {
                showTranslateDialog = false
            },
            onUseTranslation = { translatedText, _ ->
                onTextTranslated(translatedText)
            }
        )
    }
}