package com.example.aidocumentorganizer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aidocumentorganizer.data.local.DocumentEntity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(
    selected: String,
    onSelect: (String) -> Unit
) {

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DocCategory.entries.forEach { category ->
            FilterPill(
                text = category.label,
                selected = category.label.equals(selected, ignoreCase = true),
                onClick = { onSelect(category.label) }
            )
        }
    }
}

/** "Expiry date (optional)" row with a picker and a clear button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryField(
    expiry: Long?,
    onPick: (Long) -> Unit,
    onClear: () -> Unit
) {

    var showPicker by remember { mutableStateOf(false) }

    Surface(
        onClick = { showPicker = true },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                Icons.Filled.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {

                Text(
                    "Expiry date (optional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = expiry?.let { formatExpiry(it) } ?: "Tap to set a reminder date",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            if (expiry != null) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear expiry date")
                }
            }
        }
    }

    if (showPicker) {

        val state = rememberDatePickerState(initialSelectedDateMillis = expiry)

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let(onPick)
                        showPicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

/** Change a saved document's category and expiry date. */
@Composable
fun EditDetailsDialog(
    document: DocumentEntity,
    onDismiss: () -> Unit,
    onSave: (category: String, expiryDate: Long?) -> Unit
) {

    var category by remember(document.id) {
        mutableStateOf(DocCategory.fromLabel(document.category).label)
    }

    var expiry by remember(document.id) {
        mutableStateOf(document.expiryDate)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Category & expiry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                Text(
                    "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                CategoryChips(selected = category, onSelect = { category = it })

                ExpiryField(
                    expiry = expiry,
                    onPick = { expiry = it },
                    onClear = { expiry = null }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(category, expiry) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
