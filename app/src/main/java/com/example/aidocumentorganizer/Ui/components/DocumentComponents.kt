package com.example.aidocumentorganizer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.aidocumentorganizer.data.local.DocumentEntity
import java.io.File

/** Everything a document row's ⋮ menu can do. Wired up in MainActivity. */
data class DocumentActions(
    val onOpen: (DocumentEntity) -> Unit,
    val onRename: (DocumentEntity) -> Unit,
    val onEditDetails: (DocumentEntity) -> Unit,
    val onShare: (DocumentEntity) -> Unit,
    val onExportPdf: (DocumentEntity) -> Unit,
    val onDelete: (DocumentEntity) -> Unit,
    val onToggleFavorite: (DocumentEntity) -> Unit
)

// ============================================================
// DOCUMENT ROW  (icon/thumbnail · title · date • size · ⋮)
// ============================================================

@Composable
fun DocumentRow(
    document: DocumentEntity,
    actions: DocumentActions,
    modifier: Modifier = Modifier
) {

    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val category = DocCategory.fromLabel(document.category)

    val primaryImagePath = remember(document.imagePath) { document.imagePath.split("|").firstOrNull().orEmpty() }
    val sizeText = remember(primaryImagePath) { formatFileSize(primaryImagePath) }

    Surface(
        onClick = { actions.onOpen(document) },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            DocumentThumbnail(primaryImagePath, category)

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {

                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = formatDate(document.dateAdded) +
                            (sizeText?.let { " • $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Pill(category.label, category.color)

                    document.expiryDate?.let { expiry ->

                        val days = daysUntil(expiry)

                        when {
                            days < 0 ->
                                Pill("Expired", MaterialTheme.colorScheme.error)

                            days <= EXPIRY_WARNING_DAYS ->
                                Pill("Expires in ${days}d", MaterialTheme.colorScheme.tertiary)

                            else ->
                                Pill("Exp ${formatExpiry(expiry)}", MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            IconButton(onClick = { actions.onToggleFavorite(document) }) {
                Icon(
                    imageVector = if (document.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (document.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (document.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {

                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {

                    MenuItem("Open", Icons.AutoMirrored.Filled.OpenInNew) {
                        menuExpanded = false
                        actions.onOpen(document)
                    }
                    MenuItem("Rename", Icons.Filled.DriveFileRenameOutline) {
                        menuExpanded = false
                        actions.onRename(document)
                    }
                    MenuItem("Category & expiry", Icons.Filled.Event) {
                        menuExpanded = false
                        actions.onEditDetails(document)
                    }
                    MenuItem("Share text", Icons.Filled.Share) {
                        menuExpanded = false
                        actions.onShare(document)
                    }
                    MenuItem("Export as PDF", Icons.Filled.PictureAsPdf) {
                        menuExpanded = false
                        actions.onExportPdf(document)
                    }
                    MenuItem("Delete", Icons.Filled.Delete) {
                        menuExpanded = false
                        confirmDelete = true
                    }
                }
            }
        }
    }

    if (confirmDelete) {

        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete document?") },
            text = { Text("\"${document.title}\" will be permanently removed from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        actions.onDelete(document)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick
    )
}

@Composable
private fun DocumentThumbnail(imagePath: String, category: DocCategory) {

    val hasImage = remember(imagePath) {
        imagePath.isNotBlank() && File(imagePath).exists()
    }

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(category.color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {

        if (hasImage) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(category.icon, contentDescription = null, tint = category.color)
        }
    }
}

// ============================================================
// SMALL SHARED PIECES
// ============================================================

@Composable
fun Pill(text: String, color: Color) {

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1
        )
    }
}

/** Rounded filter chip like the "Today / 1 week / 1 month" row. */
@Composable
fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(6.dp))

        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NoResultsState() {
    EmptyState(
        icon = Icons.Filled.SearchOff,
        title = "No documents found",
        message = "Try a different word or clear the filters."
    )
}

@Composable
fun LogoutConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log out?") },
        text = { Text("You'll need to sign in again to see your documents.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Log out", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
