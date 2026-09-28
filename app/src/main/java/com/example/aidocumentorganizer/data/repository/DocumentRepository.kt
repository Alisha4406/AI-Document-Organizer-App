package com.example.aidocumentorganizer.data.repository

import com.example.aidocumentorganizer.data.local.DocumentDao
import com.example.aidocumentorganizer.data.local.DocumentEntity
import kotlinx.coroutines.flow.Flow

class DocumentRepository(
    private val documentDao: DocumentDao
) {

    suspend fun insertDocument(
        document: DocumentEntity
    ) {
        documentDao.insertDocument(document)
    }

    suspend fun deleteDocument(
        document: DocumentEntity
    ) {
        documentDao.deleteDocument(document)
    }

    fun getDocumentsForUser(
        userId: String
    ): Flow<List<DocumentEntity>> {

        return documentDao.getDocumentsForUser(userId)
    }

    suspend fun getDocumentById(
        id: Int,
        userId: String
    ): DocumentEntity? {

        return documentDao.getDocumentById(
            id = id,
            userId = userId
        )
    }

    suspend fun renameDocument(
        id: Int,
        newTitle: String,
        userId: String
    ) {

        documentDao.renameDocument(
            id = id,
            newTitle = newTitle,
            userId = userId
        )
    }

    suspend fun updateExtractedText(
        id: Int,
        newText: String,
        userId: String
    ) {

        documentDao.updateExtractedText(
            id = id,
            newText = newText,
            userId = userId
        )
    }

    // Returns true if this user already has a document saved
    // under this name (case-insensitive, trimmed).
    suspend fun isDocumentNameTaken(
        userId: String,
        title: String,
        excludingDocumentId: Int? = null
    ): Boolean {

        return documentDao.countDocumentsWithTitle(
            userId = userId,
            title = title.trim(),
            excludeId = excludingDocumentId
        ) > 0
    }

    suspend fun toggleFavorite(id: Int, userId: String) =
        documentDao.toggleFavorite(id, userId)

    suspend fun updateTags(id: Int, tags: String, userId: String) =
        documentDao.updateTags(id, tags, userId)

    fun getFavoriteDocuments(userId: String): Flow<List<DocumentEntity>> =
        documentDao.getFavoriteDocuments(userId)

    suspend fun updateDetails(
        id: Int,
        category: String,
        expiryDate: Long?,
        userId: String
    ) {

        documentDao.updateDetails(
            id = id,
            category = category,
            expiryDate = expiryDate,
            userId = userId
        )
    }
}