package com.example.aidocumentorganizer.Ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aidocumentorganizer.ui.theme.BrandOrange
import com.example.aidocumentorganizer.ui.theme.Indigo50
import com.example.aidocumentorganizer.ui.theme.NavPillDark
import com.example.aidocumentorganizer.ui.theme.Teal50

@Composable
private fun screenGradient(): Brush {

    val background = MaterialTheme.colorScheme.background

    return if (background.luminance() < 0.5f) {
        Brush.verticalGradient(listOf(background, background))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFE6E9FF), Color(0xFFFCEFE6)))
    }
}

/** One highlighted capability shown on the welcome screen. */
private data class Feature(
    val icon: ImageVector,
    val tint: Color,
    val title: String,
    val subtitle: String
)

private val features = listOf(
    Feature(
        Icons.Filled.DocumentScanner, Indigo50,
        "Scan & read", "Capture a page and extract its text"
    ),
    Feature(
        Icons.Filled.Folder, Teal50,
        "Auto-organize", "Documents are sorted into folders for you"
    ),
    Feature(
        Icons.Filled.Translate, BrandOrange,
        "Translate", "Understand documents in other languages"
    ),
    Feature(
        Icons.Filled.NotificationsActive, Color(0xFFDC2626),
        "Expiry reminders", "Never miss a renewal date"
    )
)

@Composable
private fun FeatureRow(feature: Feature) {

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(feature.tint.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    feature.icon,
                    contentDescription = null,
                    tint = feature.tint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    feature.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    feature.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================================
// WELCOME  (shown to signed-out users before login / sign up)
//
// The app icon is already shown by the system splash screen, so
// this screen deliberately does not repeat it - it shows what
// the app does instead.
// ============================================================

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onLogin: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenGradient())
            .statusBarsPadding()
    ) {

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                "AI Document Organizer",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                features.forEach { FeatureRow(it) }
            }
        }

        Surface(
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    "Scan Documents\nInstantly",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    "Capture, read, sort and search everything on paper - right from your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onGetStarted,
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavPillDark,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    Text("Get Started", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                }

                TextButton(onClick = onLogin) {
                    Text("I already have an account - Log in")
                }
            }
        }
    }
}
