package com.example.aidocumentorganizer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.aidocumentorganizer.ui.theme.BrandOrange
import com.example.aidocumentorganizer.ui.theme.NavPillDark

enum class MainTab { Home, Documents, Categories, Settings }

/**
 * Common frame for the four main tabs: soft gradient background and the
 * floating dark pill navigation bar with the orange "+" button.
 */
@Composable
fun MainShell(
    selected: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onAddClick: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {

    val background = MaterialTheme.colorScheme.background
    val isDark = background.luminance() < 0.5f

    val gradient =
        if (isDark) {
            Brush.verticalGradient(listOf(background, background))
        } else {
            Brush.verticalGradient(
                listOf(Color(0xFFE6E9FF), Color(0xFFF6F7FB), Color(0xFFFCEFE6))
            )
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                FloatingNavBar(
                    selected = selected,
                    onTabSelected = onTabSelected,
                    onAddClick = onAddClick
                )
            },
            content = content
        )
    }
}

@Composable
private fun FloatingNavBar(
    selected: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onAddClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {

        Row(
            modifier = Modifier
                .shadow(12.dp, RoundedCornerShape(36.dp))
                .clip(RoundedCornerShape(36.dp))
                .background(NavPillDark)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            NavItem(Icons.Filled.Home, "Home", selected == MainTab.Home) {
                onTabSelected(MainTab.Home)
            }

            NavItem(Icons.Filled.Search, "Search documents", selected == MainTab.Documents) {
                onTabSelected(MainTab.Documents)
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(BrandOrange)
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add document",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            NavItem(Icons.Filled.Folder, "Categories", selected == MainTab.Categories) {
                onTabSelected(MainTab.Categories)
            }

            NavItem(Icons.Filled.Person, "Profile and settings", selected == MainTab.Settings) {
                onTabSelected(MainTab.Settings)
            }
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) NavPillDark else Color.White.copy(alpha = 0.75f)
        )
    }
}
