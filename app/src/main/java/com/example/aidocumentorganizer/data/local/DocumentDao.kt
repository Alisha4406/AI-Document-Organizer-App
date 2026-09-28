package com.example.aidocumentorganizer.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Insert
    suspend fun insertDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("""
        UPDATE documents
        SET title = :newTitle
        WHERE id = :id
        AND userId = :userId
    """)
    suspend fun renameDocument(
        id: Int,
        newTitle: String,
        userId: String
    )

    @Query("""
        UPDATE documents
        SET extractedText = :newText
        WHERE id = :id
        AND userId = :userId
    """)
    suspend fun updateExtractedText(
        id: Int,
        newText: String,
        userId: String
    )

    @Query("""
        UPDATE documents
        SET category = :category,
            expiryDate = :expiryDate
        WHERE id = :id
        AND userId = :userId
    """)
    suspend fun updateDetails(
        id: Int,
        category: String,
        expiryDate: Long?,
        userId: String
    )

    @Query("""
        SELECT * FROM documents
        WHERE userId = :userId
        ORDER BY dateAdded DESC
    """)
    fun getDocumentsForUser(
        userId: String
    ): Flow<List<DocumentEntity>>

    @Query("""
        SELECT * FROM documents
        WHERE id = :id
        AND userId = :userId
        LIMIT 1
    """)
    suspend fun getDocumentById(
        id: Int,
        userId: String
    ): DocumentEntity?

    // Case-insensitive check used to block saving a document under
    // a name that already exists for this user.
    @Query("""
        SELECT COUNT(*) FROM documents
        WHERE userId = :userId
        AND LOWER(TRIM(title)) = LOWER(TRIM(:title))
        AND (:excludeId IS NULL OR id != :excludeId)
    """)
    suspend fun countDocumentsWithTitle(
        userId: String,
        title: String,
        excludeId: Int? = null
    ): Int

    // Used by the daily reminder worker.
    @Query("""
        UPDATE documents
        SET isFavorite = NOT isFavorite
        WHERE id = :id AND userId = :userId
    """)
    suspend fun toggleFavorite(id: Int, userId: String)

    @Query("""
        UPDATE documents
        SET tags = :tags
        WHERE id = :id AND userId = :userId
    """)
    suspend fun updateTags(id: Int, tags: String, userId: String)

    @Query("""
        SELECT * FROM documents
        WHERE userId = :userId
        AND isFavorite = 1
        ORDER BY dateAdded DESC
    """)
    fun getFavoriteDocuments(userId: String): Flow<List<DocumentEntity>>

    @Query("""
        SELECT * FROM documents
        WHERE userId = :userId
        AND expiryDate IS NOT NULL
        AND expiryDate BETWEEN :from AND :to
        ORDER BY expiryDate ASC
    """)
    suspend fun getDocumentsExpiringBetween(
        userId: String,
        from: Long,
        to: Long
    ): List<DocumentEntity>
}
