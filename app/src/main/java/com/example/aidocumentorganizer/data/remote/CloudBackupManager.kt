package com.example.aidocumentorganizer.data.remote

import android.net.Uri
import com.example.aidocumentorganizer.data.local.DocumentEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Explicit, user-triggered cloud backup.
 *
 * Room remains the offline source of truth. Firestore stores searchable
 * document metadata/OCR and Firebase Storage stores the original pages.
 */
class CloudBackupManager(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    suspend fun backupDocuments(
        userId: String,
        documents: List<DocumentEntity>
    ): BackupResult {
        require(userId.isNotBlank()) { "A signed-in user is required." }

        var uploadedDocuments = 0
        var uploadedPages = 0

        documents.forEach { document ->
            val pagePaths = document.imagePath
                .split("|")
                .map(String::trim)
                .filter(String::isNotBlank)

            val pageUrls = mutableListOf<String>()

            pagePaths.forEachIndexed { index, path ->
                val file = File(path)
                if (!file.exists()) return@forEachIndexed

                val ref = storage.reference
                    .child("users/$userId/documents/${document.id}/page_${index + 1}.jpg")

                ref.putFile(Uri.fromFile(file)).await()
                pageUrls += ref.downloadUrl.await().toString()
                uploadedPages++
            }

            val data = mapOf(
                "id" to document.id,
                "userId" to userId,
                "title" to document.title,
                "type" to document.type,
                "category" to document.category,
                "tags" to document.tags,
                "isFavorite" to document.isFavorite,
                "extractedText" to document.extractedText,
                "expiryDate" to document.expiryDate,
                "dateAdded" to document.dateAdded,
                "pageCount" to document.pageCount,
                "imageUrls" to pageUrls
            )

            firestore.collection("users")
                .document(userId)
                .collection("documents")
                .document(document.id.toString())
                .set(data)
                .await()

            uploadedDocuments++
        }

        return BackupResult(uploadedDocuments, uploadedPages)
    }
}

data class BackupResult(
    val documents: Int,
    val pages: Int
)
