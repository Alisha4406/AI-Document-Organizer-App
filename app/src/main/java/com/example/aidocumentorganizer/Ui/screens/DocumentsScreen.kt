package com.example.aidocumentorganizer.Ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aidocumentorganizer.ui.components.DAY_MS
import com.example.aidocumentorganizer.ui.components.DocCategory
import com.example.aidocumentorganizer.ui.components.DocumentActions
import com.example.aidocumentorganizer.ui.components.DocumentRow
import com.example.aidocumentorganizer.ui.components.EmptyState
import com.example.aidocumentorganizer.ui.components.FilterPill
import com.example.aidocumentorganizer.ui.components.MainShell
import com.example.aidocumentorganizer.ui.components.MainTab
import com.example.aidocumentorganizer.ui.components.NoResultsState
import com.example.aidocumentorganizer.ui.components.isExpiringSoon
import com.example.aidocumentorganizer.ui.home.HomeViewModel
import java.util.Calendar

private enum class TimeFilter(val label: String) {
    ALL("All"), TODAY("Today"), WEEK("1 week"), MONTH("1 month"), THREE_MONTHS("3 months")
}

private enum class SortOption(val label: String) {
    NEWEST("Newest first"), OLDEST("Oldest first"), A_TO_Z("A-Z"), EXPIRY("Expiry date")
}

private fun startOfToday(): Long =
    Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/**
 * "My Documents" + smart search in one screen.
 * Search looks at title, OCR text AND category, and every word you type
 * must match somewhere, so "fee challan" finds a document even if the
 * two words are not next to each other.
 */
@Composable
fun DocumentsScreen(
    viewModel: HomeViewModel,
    initialCategory: String?,
    initialExpiringOnly: Boolean,
    actions: DocumentActions,
    onTabSelected: (MainTab) -> Unit,
    onAddClick: () -> Unit
) {

    val documents by viewModel.documents.collectAsStateWithLifecycle()

    var query by rememberSaveable { mutableStateOf("") }

    var timeFilter by remember { mutableStateOf(TimeFilter.ALL) }

    var category by remember(initialCategory, initialExpiringOnly) {
        mutableStateOf(initialCategory)
    }

    var expiringOnly by remember(initialCategory, initialExpiringOnly) { mutableStateOf(initialExpiringOnly) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var sortOption by rememberSaveable { mutableStateOf(SortOption.NEWEST.name) }
    var sortMenu by remember { mutableStateOf(false) }

    val results = remember(documents, query, timeFilter, category, expiringOnly, favoritesOnly, sortOption) {

        val now = System.currentTimeMillis()

        val cutoff: Long? = when (timeFilter) {
            TimeFilter.ALL -> null
            TimeFilter.TODAY -> startOfToday()
            TimeFilter.WEEK -> now - 7 * DAY_MS
            TimeFilter.MONTH -> now - 30 * DAY_MS
            TimeFilter.THREE_MONTHS -> now - 90 * DAY_MS
        }

        val tokens = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }

        documents.filter { document ->

            (cutoff == null || document.dateAdded >= cutoff) &&

                    (category == null ||
                            document.category.equals(category, ignoreCase = true)) &&

                    (!expiringOnly || isExpiringSoon(document.expiryDate)) &&
                    (!favoritesOnly || document.isFavorite) &&

                    (tokens.isEmpty() || run {
                        val haystack =
                            (document.title + " " + document.extractedText + " " + document.category + " " + document.tags)
                                .lowercase()
                        tokens.all { haystack.contains(it) }
                    })
        }.let { filtered ->
            when (SortOption.valueOf(sortOption)) {
                SortOption.NEWEST -> filtered.sortedByDescending { it.dateAdded }
                SortOption.OLDEST -> filtered.sortedBy { it.dateAdded }
                SortOption.A_TO_Z -> filtered.sortedBy { it.title.lowercase() }
                SortOption.EXPIRY -> filtered.sortedWith(compareBy({ it.expiryDate == null }, { it.expiryDate ?: Long.MAX_VALUE }))
            }
        }
    }

    MainShell(
        selected = MainTab.Documents,
        onTabSelected = onTabSelected,
        onAddClick = onAddClick
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Text(
                "My Documents",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)
            )

            Text(
                "${results.size} of ${documents.size} shown",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search documents...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(30.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Time chips: Today / 1 week / 1 month / 3 months
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TimeFilter.entries.toList()) { filter ->
                    FilterPill(
                        text = filter.label,
                        selected = timeFilter == filter,
                        onClick = { timeFilter = filter }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Category chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                item {
                    FilterPill(
                        text = "All types",
                        selected = category == null && !expiringOnly,
                        onClick = {
                            category = null
                            expiringOnly = false
                        }
                    )
                }

                items(DocCategory.entries.toList()) { entry ->
                    FilterPill(
                        text = entry.label,
                        selected = category.equals(entry.label, ignoreCase = true),
                        onClick = {
                            category = if (category.equals(entry.label, ignoreCase = true)) null
                            else entry.label
                        }
                    )
                }

                item {
                    FilterPill(
                        text = "Expiring soon",
                        selected = expiringOnly,
                        onClick = { expiringOnly = !expiringOnly }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterPill(
                        text = "⭐ Favorites",
                        selected = favoritesOnly,
                        onClick = { favoritesOnly = !favoritesOnly }
                    )
                }
                item {
                    Box {
                        FilterPill(
                            text = "Sort: ${SortOption.valueOf(sortOption).label}",
                            selected = false,
                            onClick = { sortMenu = true }
                        )
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = { sortOption = option.name; sortMenu = false }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                when {
                    documents.isEmpty() -> item {
                        EmptyState(
                            icon = Icons.Filled.Description,
                            title = "No documents yet",
                            message = "Tap the orange + button to scan or import your first document."
                        )
                    }

                    results.isEmpty() -> item { NoResultsState() }

                    else -> items(results, key = { it.id }) { document ->
                        DocumentRow(document = document, actions = actions)
                    }
                }
            }
        }
    }
}
