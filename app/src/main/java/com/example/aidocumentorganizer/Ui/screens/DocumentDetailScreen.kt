package com.example.aidocumentorganizer.Ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.aidocumentorganizer.R
import com.example.aidocumentorganizer.data.local.DocumentEntity
import com.example.aidocumentorganizer.utils.DocumentInsightsEngine
import com.example.aidocumentorganizer.utils.DocumentQuestionEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    document: DocumentEntity,
    onBack: () -> Unit,
    onRename: (String, (Boolean) -> Unit) -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    onUpdateTags: ((String) -> Unit)? = null,
    // Optional: lets the extracted text itself be corrected in
    // place - useful when OCR misreads a word or two. Pass
    // nothing to hide the Edit option.
    onEditExtractedText: ((String) -> Unit)? = null,
    // Optional: lets the translated text be saved as a brand new
    // document instead of just being shown. Pass nothing to hide
    // that option and only show the translation on screen.
    onSaveTranslatedCopy: ((translatedText: String, languageName: String) -> Unit)? = null
) {

    val context = LocalContext.current

    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTranslateDialog by remember { mutableStateOf(false) }
    var showEditTextDialog by remember { mutableStateOf(false) }
    var newDocumentName by remember { mutableStateOf(document.title) }
    var renameErrorMessage by remember { mutableStateOf<String?>(null) }
    var editedExtractedText by remember { mutableStateOf(document.extractedText) }
    var showTagsDialog by remember { mutableStateOf(false) }
    var editedTags by remember { mutableStateOf(document.tags) }

    val scrollState = rememberScrollState()
    val primaryImagePath = remember(document.imagePath) { document.imagePath.split("|").firstOrNull().orEmpty() }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        text = "Document Details",
                        fontWeight = FontWeight.SemiBold
                    )
                },

                navigationIcon = {

                    IconButton(onClick = onBack) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },

        containerColor = MaterialTheme.colorScheme.background

    ) { innerPadding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),

            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // ==========================================
            // DOCUMENT IMAGE / PREVIEW
            // ==========================================

            if (primaryImagePath.isNotBlank()) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {

                    AsyncImage(
                        model = primaryImagePath,
                        contentDescription = "Document image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentScale = ContentScale.Fit
                    )
                }

            } else {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        Image(
                            painter = painterResource(R.drawable.doc),
                            contentDescription = "Document",
                            modifier = Modifier.height(72.dp)
                        )
                    }
                }
            }


            // ==========================================
            // TITLE + TYPE BADGE
            // ==========================================

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    if (onToggleFavorite != null) {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (document.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (document.isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (document.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {

                        Row(
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 4.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {

                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(14.dp)
                            )

                            Text(
                                text = document.type,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }


            if (document.tags.isNotBlank() || onUpdateTags != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    onClick = { if (onUpdateTags != null) showTagsDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Filled.LocalOffer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(Modifier.weight(1f)) {
                            Text("Tags", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text(
                                document.tags.ifBlank { "Add tags to find this document faster" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (onUpdateTags != null) {
                            TextButton(onClick = { showTagsDialog = true }) { Text("Edit") }
                        }
                    }
                }
            }

            // ==========================================
            // DOCUMENT INTELLIGENCE
            // ==========================================

            val insights = remember(document.id, document.extractedText, document.category) {
                DocumentInsightsEngine.analyze(
                    title = document.title,
                    category = document.category,
                    text = document.extractedText
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Document Intelligence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(insights.summary, style = MaterialTheme.typography.bodyMedium)
                    insights.keyFacts.forEach { fact ->
                        Text("• $fact", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // ==========================================
            // ASK THIS DOCUMENT (offline quick answers)
            // ==========================================

            var question by remember { mutableStateOf("") }
            var answer by remember { mutableStateOf<String?>(null) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ask this document", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Get quick answers from the OCR text without sending the document online.", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("What is the amount?", "What dates are mentioned?").forEach { prompt ->
                            OutlinedButton(onClick = { question = prompt }) { Text(prompt, maxLines = 1) }
                        }
                    }
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Your question") }
                    )
                    Button(
                        onClick = {
                            answer = DocumentQuestionEngine.answer(
                                question = question,
                                text = document.extractedText,
                                category = document.category,
                                summary = insights.summary
                            )
                        },
                        enabled = question.isNotBlank()
                    ) { Text("Ask") }
                    answer?.let {
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // ==========================================
            // EXTRACTED TEXT
            // ==========================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {

                Column(modifier = Modifier.padding(18.dp)) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "Extracted Text",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (onEditExtractedText != null) {

                            IconButton(
                                onClick = {
                                    editedExtractedText = document.extractedText
                                    showEditTextDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EditNote,
                                    contentDescription = "Edit extracted text",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (document.extractedText.isBlank()) {
                            "No extracted text available."
                        } else {
                            document.extractedText
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (document.extractedText.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }


            // ==========================================
            // PRIMARY ACTIONS — Rename / Share, side by side
            // ==========================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                FilledTonalButton(
                    onClick = {
                        newDocumentName = document.title
                        renameErrorMessage = null
                        showRenameDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DriveFileRenameOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rename")
                }

                FilledTonalButton(
                    onClick = {

                        val shareText = """
                            ${document.title}
                            
                            ${document.extractedText}
                        """.trimIndent()

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }

                        context.startActivity(
                            Intent.createChooser(shareIntent, "Share Document")
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.IosShare,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }
            }


            // ==========================================
            // TRANSLATE BUTTON
            // ==========================================

            OutlinedButton(
                onClick = { showTranslateDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = document.extractedText.isNotBlank(),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Translate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Translate")
            }


            // ==========================================
            // DELETE BUTTON
            // ==========================================

            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete")
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }


    // ==============================================
    // RENAME DIALOG
    // ==============================================

    if (showRenameDialog) {

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Document") },
            text = {

                OutlinedTextField(
                    value = newDocumentName,
                    onValueChange = {
                        newDocumentName = it
                        renameErrorMessage = null
                    },
                    label = { Text("Document Name") },
                    placeholder = { Text("Enter document name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = renameErrorMessage != null,
                    supportingText = { renameErrorMessage?.let { Text(it) } }
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        val name = newDocumentName.trim()

                        if (name.isEmpty()) {
                            renameErrorMessage = "Document name cannot be empty."
                        } else {
                            onRename(name) { success ->
                                if (success) {
                                    renameErrorMessage = null
                                    showRenameDialog = false
                                } else {
                                    renameErrorMessage = "Another document already uses this name."
                                }
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false; renameErrorMessage = null }) {
                    Text("Cancel")
                }
            }
        )
    }


    // ==============================================
    // DELETE DIALOG
    // ==============================================

    if (showDeleteDialog) {

        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Document") },
            text = { Text("Are you sure you want to delete this document?") },
            confirmButton = {

                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTagsDialog && onUpdateTags != null) {
        AlertDialog(
            onDismissRequest = { showTagsDialog = false },
            title = { Text("Document tags") },
            text = {
                OutlinedTextField(
                    value = editedTags,
                    onValueChange = { editedTags = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    label = { Text("Tags") },
                    placeholder = { Text("passport, travel, 2026") },
                    supportingText = { Text("Separate tags with commas") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateTags?.invoke(editedTags)
                    showTagsDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showTagsDialog = false }) { Text("Cancel") }
            }
        )
    }


    // ==============================================
    // EDIT EXTRACTED TEXT DIALOG
    // ==============================================

    if (showEditTextDialog && onEditExtractedText != null) {

        AlertDialog(
            onDismissRequest = { showEditTextDialog = false },
            title = { Text("Edit Extracted Text") },
            text = {

                OutlinedTextField(
                    value = editedExtractedText,
                    onValueChange = { editedExtractedText = it },
                    placeholder = { Text("Document text") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 360.dp)
                        .verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {
                        onEditExtractedText(editedExtractedText)
                        showEditTextDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        editedExtractedText = document.extractedText
                        showEditTextDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }


    // ==============================================
    // TRANSLATE DIALOG
    // ==============================================

    if (showTranslateDialog) {

        TranslateDialog(
            sourceText = document.extractedText,
            onDismiss = { showTranslateDialog = false },
            onUseTranslation = if (onSaveTranslatedCopy != null) {
                { translatedText, languageName ->
                    onSaveTranslatedCopy(translatedText, languageName)
                }
            } else {
                null
            }
        )
    }
}
