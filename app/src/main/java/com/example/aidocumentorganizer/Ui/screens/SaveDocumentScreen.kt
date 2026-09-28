package com.example.aidocumentorganizer.Ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.aidocumentorganizer.ui.components.CategoryChips
import com.example.aidocumentorganizer.ui.components.ExpiryField
import com.example.aidocumentorganizer.utils.DocumentClassifier

/**
 * Final step before a scan is stored. The name and category are pre-filled
 * by the on-device classifier from the OCR text, and the user can change
 * either one - plus optionally set an expiry date for reminders.
 */
@Composable
fun SaveDocumentScreen(
    extractedText: String,
    onSave: (name: String, category: String, expiryDate: Long?) -> Unit,
    onBack: () -> Unit,
    // Set by the caller when the name just submitted already
    // exists for this user. Shown under the text field and
    // cleared automatically as soon as the user edits the name.
    errorMessage: String? = null,
    onNameChanged: () -> Unit = {}
) {

    val suggestedCategory = remember(extractedText) {
        DocumentClassifier.classify(extractedText)
    }

    var documentName by remember(extractedText) {
        mutableStateOf(DocumentClassifier.suggestTitle(extractedText, suggestedCategory))
    }

    var category by remember(extractedText) { mutableStateOf(suggestedCategory) }

    var expiry by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Keep the arrow below the status bar / camera cutout;
                    // taps on the status bar never reach the app.
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Save Document",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // ---- Name ----
            OutlinedTextField(
                value = documentName,
                onValueChange = {
                    documentName = it
                    // Any edit invalidates a previous "name already
                    // exists" error so it doesn't linger.
                    onNameChanged()
                },
                label = { Text("Document Name") },
                placeholder = { Text("Enter document name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                isError = errorMessage != null,
                supportingText = {
                    if (errorMessage != null) {
                        Text(errorMessage, color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            // ---- Category ----
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Text(
                        "Category",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (category == suggestedCategory) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Suggested automatically",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                CategoryChips(selected = category, onSelect = { category = it })
            }

            // ---- Expiry ----
            ExpiryField(
                expiry = expiry,
                onPick = { expiry = it },
                onClear = { expiry = null }
            )

            // ---- Text preview ----
            if (extractedText.isNotBlank()) {

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                    Text(
                        "Extracted text preview",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        extractedText,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    if (documentName.isNotBlank()) {
                        onSave(documentName.trim(), category, expiry)
                    }
                },
                enabled = documentName.isNotBlank(),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text("Save Document", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
