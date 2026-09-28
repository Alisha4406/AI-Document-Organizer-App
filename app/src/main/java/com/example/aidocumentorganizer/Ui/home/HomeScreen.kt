package com.example.aidocumentorganizer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aidocumentorganizer.ui.components.DAY_MS
import com.example.aidocumentorganizer.ui.components.DocCategory
import com.example.aidocumentorganizer.ui.components.DocumentActions
import com.example.aidocumentorganizer.ui.components.DocumentRow
import com.example.aidocumentorganizer.ui.components.EmptyState
import com.example.aidocumentorganizer.ui.components.LogoutConfirmDialog
import com.example.aidocumentorganizer.ui.components.MainShell
import com.example.aidocumentorganizer.ui.components.MainTab
import com.example.aidocumentorganizer.ui.components.isExpiringSoon
import com.example.aidocumentorganizer.ui.theme.BrandOrange
import com.example.aidocumentorganizer.ui.theme.BrandOrangeDeep
import com.example.aidocumentorganizer.ui.theme.NavPillDark

// ============================================================
// HOME DASHBOARD
// ============================================================

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    userName: String,
    actions: DocumentActions,
    onTabSelected: (MainTab) -> Unit,
    onAddDocumentClick: () -> Unit,
    onScanClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSeeAllClick: () -> Unit,
    onExpiringClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onLogoutClick: () -> Unit
) {

    val documents by viewModel.documents.collectAsStateWithLifecycle()

    val now = System.currentTimeMillis()

    val thisWeek = remember(documents) {
        documents.count { it.dateAdded >= now - 7 * DAY_MS }
    }

    val expiringSoon = remember(documents) {
        documents.count { isExpiringSoon(it.expiryDate) }
    }

    val topCategories = remember(documents) {
        documents
            .groupBy { DocCategory.fromLabel(it.category) }
            .map { (category, list) -> category to list.size }
            .sortedByDescending { it.second }
            .take(2)
    }

    var showLogoutConfirm by remember { mutableStateOf(false) }

    MainShell(
        selected = MainTab.Home,
        onTabSelected = onTabSelected,
        onAddClick = onAddDocumentClick
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ---------------- Header ----------------
            item {
                HomeHeader(
                    userName = userName,
                    documentCount = documents.size,
                    hasAlerts = expiringSoon > 0,
                    onProfileClick = { onTabSelected(MainTab.Settings) },
                    onLogoutClick = { showLogoutConfirm = true },
                    onBellClick = onExpiringClick
                )
            }

            // ---------------- Headline ----------------
            item {
                Text(
                    text = "How can I help\nyou today?",
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // ---------------- Search bar ----------------
            item { SearchBarButton(onClick = onSearchClick) }

            // ---------------- Orange summary card ----------------
            item {
                RecentFoldersCard(
                    topCategories = topCategories,
                    onCategoryClick = onCategoryClick
                )
            }

            // ---------------- Quick actions ----------------
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionTile(
                        icon = Icons.Filled.CameraAlt,
                        title = "Scan",
                        subtitle = "Camera + OCR",
                        onClick = onScanClick,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Filled.PhotoLibrary,
                        title = "Import",
                        subtitle = "Images or PDF",
                        onClick = onGalleryClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ---------------- Stats ----------------
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("${documents.size}", "Total", Modifier.weight(1f))
                    StatTile("$thisWeek", "This week", Modifier.weight(1f))
                    StatTile(
                        "$expiringSoon", "Expiring",
                        Modifier.weight(1f),
                        highlight = expiringSoon > 0,
                        onClick = onExpiringClick
                    )
                }
            }

            // ---------------- Recent documents ----------------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Documents",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (documents.isNotEmpty()) {
                        TextButton(onClick = onSeeAllClick) { Text("See all") }
                    }
                }
            }

            if (documents.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Description,
                        title = "No documents yet",
                        message = "Tap the orange + button to scan or import your first document."
                    )
                }
            } else {
                items(documents.take(5), key = { it.id }) { document ->
                    DocumentRow(document = document, actions = actions)
                }
            }
        }
    }

    if (showLogoutConfirm) {
        LogoutConfirmDialog(
            onConfirm = {
                showLogoutConfirm = false
                onLogoutClick()
            },
            onDismiss = { showLogoutConfirm = false }
        )
    }
}

// ============================================================
// PIECES
// ============================================================

@Composable
private fun HomeHeader(
    userName: String,
    documentCount: Int,
    hasAlerts: Boolean,
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onBellClick: () -> Unit
) {

    var menuOpen by remember { mutableStateOf(false) }

    val displayName = userName.ifBlank { "there" }
    val initial = displayName.first().uppercase()

    Row(verticalAlignment = Alignment.CenterVertically) {

        // Avatar with a small profile / logout menu
        Box {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { menuOpen = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initial,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {

                DropdownMenuItem(
                    text = { Text("Profile & settings") },
                    leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onProfileClick()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Log out", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onLogoutClick()
                    }
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                "Hello, $displayName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                "$documentCount document${if (documentCount == 1) "" else "s"} organized",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box {
            Surface(
                onClick = onBellClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Expiring documents")
                }
            }

            if (hasAlerts) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}

@Composable
private fun SearchBarButton(onClick: () -> Unit) {

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {

        Row(
            modifier = Modifier.padding(start = 18.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.width(12.dp))

            Text(
                "Search title, text or category",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NavPillDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Tune,
                    contentDescription = "Filters",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentFoldersCard(
    topCategories: List<Pair<DocCategory, Int>>,
    onCategoryClick: (String) -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(BrandOrange, BrandOrangeDeep)))
            .padding(20.dp)
    ) {

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

            Text(
                "Recent files & folders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            if (topCategories.isEmpty()) {

                Text(
                    "Scan your first document and it will be sorted into a folder automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )

            } else {

                topCategories.forEach { (category, count) ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategoryClick(category.label) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.92f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Folder,
                                contentDescription = null,
                                tint = BrandOrangeDeep,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Text(
                            category.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            "$count item${if (count == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp)) {

            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(14.dp))

            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null
) {

    val container =
        if (highlight) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.surface

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            color = container,
            modifier = modifier,
            content = content
        )
    } else {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = container,
            modifier = modifier,
            content = content
        )
    }
}
