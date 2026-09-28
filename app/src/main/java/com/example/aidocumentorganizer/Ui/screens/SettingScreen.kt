package com.example.aidocumentorganizer.Ui.screens

import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aidocumentorganizer.ui.components.LogoutConfirmDialog
import com.example.aidocumentorganizer.ui.components.MainShell
import com.example.aidocumentorganizer.ui.components.MainTab
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun SettingsScreen(
    userName: String,
    userEmail: String,
    documentCount: Int,
    darkMode: Boolean?,
    remindersEnabled: Boolean,
    backupInProgress: Boolean,
    onBackupNow: () -> Unit,
    onDarkModeChange: (Boolean?) -> Unit,
    onRemindersChange: (Boolean) -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    onTabSelected: (MainTab) -> Unit,
    onAddClick: () -> Unit
) {

    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    val displayName = userName.ifBlank { "Your account" }

    MainShell(
        selected = MainTab.Settings,
        onTabSelected = onTabSelected,
        onAddClick = onAddClick
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )

            // ---------------- Profile ----------------
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            displayName.first().uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column {
                        Text(
                            displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            userEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---------------- Account ----------------
            SettingsGroup("Account") {
                SettingsRow(
                    icon = Icons.Filled.Lock,
                    title = "Change password",
                    subtitle = "Send a reset link to your email",
                    onClick = onChangePassword
                )
            }

            // ---------------- Preferences ----------------
            SettingsGroup("Preferences") {

                SettingsRow(
                    icon = Icons.Filled.NotificationsActive,
                    title = "Document reminders",
                    subtitle = "Notify me before a document expires",
                    trailing = {
                        Switch(checked = remindersEnabled, onCheckedChange = onRemindersChange)
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                SettingsRow(
                    icon = Icons.Filled.DarkMode,
                    title = "Appearance",
                    subtitle = "Choose how the app follows your device theme"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = darkMode == null,
                        onClick = { onDarkModeChange(null) },
                        label = { Text("System") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = darkMode == false,
                        onClick = { onDarkModeChange(false) },
                        label = { Text("Light") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = darkMode == true,
                        onClick = { onDarkModeChange(true) },
                        label = { Text("Dark") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ---------------- Backup ----------------
            SettingsGroup("Backup & Sync") {
                SettingsRow(
                    icon = Icons.Filled.CloudUpload,
                    title = "Cloud backup",
                    subtitle = if (backupInProgress) "Uploading your documents securely..." else "Back up document metadata, OCR text and scanned pages",
                    trailing = {
                        Button(
                            onClick = onBackupNow,
                            enabled = !backupInProgress,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(if (backupInProgress) "Backing up" else "Back up")
                        }
                    }
                )
            }

            // ---------------- Storage & about ----------------
            SettingsGroup("General") {

                SettingsRow(
                    icon = Icons.Filled.Storage,
                    title = "Storage",
                    subtitle = "$documentCount document${if (documentCount == 1) "" else "s"} stored on this device"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                SettingsRow(
                    icon = Icons.Filled.PrivacyTip,
                    title = "Privacy",
                    onClick = { showPrivacy = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                SettingsRow(
                    icon = Icons.Filled.Info,
                    title = "About app",
                    onClick = { showAbout = true }
                )
            }

            // ---------------- Logout ----------------
            Button(
                onClick = { showLogoutConfirm = true },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Log out", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showLogoutConfirm) {
        LogoutConfirmDialog(
            onConfirm = {
                showLogoutConfirm = false
                onLogout()
            },
            onDismiss = { showLogoutConfirm = false }
        )
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text("Privacy") },
            text = {
                Text(
                    "By default, scanned images and extracted text stay on this device. " +
                            "Text is read on-device using ML Kit. If you tap Cloud backup, " +
                            "the selected documents are uploaded to your Firebase project " +
                            "under your account; sharing and PDF export are also user-initiated."
                )
            },
            confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text("Close") } }
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("AI Document Organizer") },
            text = {
                Text(
                    "Version ${appVersion(LocalContext.current)}\n\n" +
                            "Scan, extract, organize, search and manage your documents. " +
                            "Built with Kotlin, Jetpack Compose, CameraX, ML Kit, Room and Firebase."
                )
            },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Close") } }
        )
    }
}

private fun appVersion(context: Context): String =
    try {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName ?: "1.0"
    } catch (e: Exception) {
        "1.0"
    }

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {

    Column {

        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
        )

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {

    val body: @Composable () -> Unit = {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when {
                trailing != null -> trailing()
                onClick != null -> Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (onClick != null) {
        Surface(onClick = onClick, color = androidx.compose.ui.graphics.Color.Transparent, content = body)
    } else {
        body()
    }
}
