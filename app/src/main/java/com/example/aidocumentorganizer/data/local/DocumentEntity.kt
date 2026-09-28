package com.example.aidocumentorganizer.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val userId: String,

    val title: String,

    val type: String,

    val extractedText: String,

    val imagePath: String,

    val dateAdded: Long,

    // Added in DB version 3 -----------------------------------------
    // Set automatically by DocumentClassifier, editable by the user.
    @ColumnInfo(defaultValue = "'Other'")
    val category: String = "Other",

    // Optional expiry (CNIC, passport, licence, insurance...). Stored as
    // UTC-midnight millis, which is what the Material date picker returns.
    val expiryDate: Long? = null,

    // Internship-level organizer features. These are local, fast and safe
    // to use even when the device is offline.
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false,

    @ColumnInfo(defaultValue = "''")
    val tags: String = "",

    // Number of scanned pages stored in imagePath (pipe-separated paths).
    @ColumnInfo(defaultValue = "1")
    val pageCount: Int = 1
)
