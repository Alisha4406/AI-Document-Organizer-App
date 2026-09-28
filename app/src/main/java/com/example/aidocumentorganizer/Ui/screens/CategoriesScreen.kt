package com.example.aidocumentorganizer.Ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aidocumentorganizer.ui.components.DocCategory
import com.example.aidocumentorganizer.ui.components.MainShell
import com.example.aidocumentorganizer.ui.components.MainTab
import com.example.aidocumentorganizer.ui.components.formatDate
import com.example.aidocumentorganizer.ui.components.isExpiringSoon
import com.example.aidocumentorganizer.ui.home.HomeViewModel

/** "My Folders": one folder per category with a live item count. */
@Composable
fun CategoriesScreen(
    viewModel: HomeViewModel,
    onCategoryClick: (String) -> Unit,
    onAllDocumentsClick: () -> Unit,
    onExpiringClick: () -> Unit,
    onTabSelected: (MainTab) -> Unit,
    onAddClick: () -> Unit
) {

    val documents by viewModel.documents.collectAsStateWithLifecycle()

    val byCategory = remember(documents) {
        documents.groupBy { DocCategory.fromLabel(it.category) }
    }

    val expiringCount = remember(documents) {
        documents.count { isExpiringSoon(it.expiryDate) }
    }

    MainShell(
        selected = MainTab.Categories,
        onTabSelected = onTabSelected,
        onAddClick = onAddClick
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                Text(
                    "Categories",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Documents are sorted automatically after each scan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Summary card with two shortcuts
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp)) {

                        Text(
                            "Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                            SummaryTile(
                                icon = Icons.Filled.Description,
                                title = "All documents",
                                count = documents.size,
                                onClick = onAllDocumentsClick,
                                modifier = Modifier.weight(1f)
                            )

                            SummaryTile(
                                icon = Icons.Filled.Event,
                                title = "Expiring soon",
                                count = expiringCount,
                                onClick = onExpiringClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "My Folders",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    DocCategory.entries.forEach { category ->

                        val list = byCategory[category].orEmpty()

                        FolderRow(
                            category = category,
                            count = list.size,
                            lastAdded = list.maxOfOrNull { it.dateAdded },
                            onClick = { onCategoryClick(category.label) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(
    icon: ImageVector,
    title: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp)) {

            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(14.dp))

            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            Text(
                "$count item${if (count == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FolderRow(
    category: DocCategory,
    count: Int,
    lastAdded: Long?,
    onClick: () -> Unit
) {

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(category.color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    category.icon,
                    contentDescription = null,
                    tint = category.color,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {

                Text(
                    category.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "$count item${if (count == 1) "" else "s"} • " +
                            (lastAdded?.let { "Last added ${formatDate(it)}" } ?: "Empty"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
