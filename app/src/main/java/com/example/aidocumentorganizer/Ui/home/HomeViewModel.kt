package com.example.aidocumentorganizer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidocumentorganizer.data.local.DocumentEntity
import com.example.aidocumentorganizer.data.repository.DocumentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: DocumentRepository,
    private val userId: String
) : ViewModel() {

    // =========================================================
    // DOCUMENTS FOR CURRENT USER ONLY
    // =========================================================

    val documents: StateFlow<List<DocumentEntity>> =
        repository
            .getDocumentsForUser(userId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // =========================================================
    // INSERT
    // =========================================================

    fun insertDocument(document: DocumentEntity) {

        viewModelScope.launch {

            // Always force the current Firebase user's UID
            val userDocument = document.copy(
                userId = userId
            )

            repository.insertDocument(userDocument)
        }
    }

    // =========================================================
    // DELETE
    // =========================================================

    fun deleteDocument(document: DocumentEntity) {

        viewModelScope.launch {

            // Make sure we only delete a document belonging
            // to the currently logged-in user.
            if (document.userId == userId) {

                repository.deleteDocument(document)
            }
        }
    }

    // =========================================================
    // RENAME
    // =========================================================

    fun renameDocument(
        id: Int,
        newTitle: String
    ) {

        viewModelScope.launch {

            repository.renameDocument(
                id = id,
                newTitle = newTitle,
                userId = userId
            )
        }
    }

    // =========================================================
    // EDIT EXTRACTED TEXT
    // =========================================================

    fun updateExtractedText(
        id: Int,
        newText: String
    ) {

        viewModelScope.launch {

            repository.updateExtractedText(
                id = id,
                newText = newText,
                userId = userId
            )
        }
    }

    // =========================================================
    // CHECK FOR DUPLICATE NAME
    //
    // Used before saving/renaming a document so the user gets
    // told the name is already in use instead of silently
    // creating a second document with the same title.
    // =========================================================

    suspend fun isDocumentNameTaken(
        title: String,
        excludingDocumentId: Int? = null
    ): Boolean {

        return repository.isDocumentNameTaken(
            userId = userId,
            title = title,
            excludingDocumentId = excludingDocumentId
        )
    }

    // =========================================================
    // UPDATE CATEGORY / EXPIRY DATE
    // =========================================================

    fun updateDetails(
        id: Int,
        category: String,
        expiryDate: Long?
    ) {

        viewModelScope.launch {

            repository.updateDetails(
                id = id,
                category = category,
                expiryDate = expiryDate,
                userId = userId
            )
        }
    }

    val favoriteDocuments: StateFlow<List<DocumentEntity>> =
        repository
            .getFavoriteDocuments(userId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun toggleFavorite(document: DocumentEntity) {
        if (document.userId != userId) return
        viewModelScope.launch { repository.toggleFavorite(document.id, userId) }
    }

    fun updateTags(id: Int, tags: String) {
        viewModelScope.launch {
            repository.updateTags(id, tags.trim(), userId)
        }
    }

    // =========================================================
    // GET ONE DOCUMENT
    // =========================================================

    fun getDocumentById(
        id: Int,
        onResult: (DocumentEntity?) -> Unit
    ) {

        viewModelScope.launch {

            val document =
                repository.getDocumentById(
                    id = id,
                    userId = userId
                )

            onResult(document)
        }
    }
}