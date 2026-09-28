package com.example.aidocumentorganizer.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.aidocumentorganizer.utils.DocCategories

/** UI look (icon + folder colour) for each stored category label. */
enum class DocCategory(
    val label: String,
    val icon: ImageVector,
    val color: Color
) {
    EDUCATION(DocCategories.EDUCATION, Icons.Filled.School, Color(0xFF4F46E5)),
    BILLS(DocCategories.BILLS, Icons.Filled.Receipt, Color(0xFFDC2626)),
    IDS(DocCategories.IDS, Icons.Filled.Badge, Color(0xFF0D9488)),
    MEDICAL(DocCategories.MEDICAL, Icons.Filled.MedicalServices, Color(0xFF16A34A)),
    WORK(DocCategories.WORK, Icons.Filled.Work, Color(0xFF2563EB)),
    OTHER(DocCategories.OTHER, Icons.Filled.Folder, Color(0xFFD97706));

    companion object {
        fun fromLabel(label: String): DocCategory =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: OTHER
    }
}
