package com.example.dibina.data

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing Firebase Storage assets.
 * Path: users/{uid}/profile/
 * Supported image formats: JPEG, PNG, WEBP
 * Enforces: Student may only modify their own profile image.
 */
class StorageRepository(
    private val storage: FirebaseStorage? = runCatching { FirebaseStorage.getInstance() }.getOrNull()
) {

    /**
     * Upload student profile image to users/{uid}/profile/avatar.<ext>
     * Returns download URL.
     */
    suspend fun uploadProfileImage(
        uid: String,
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): Result<String> {
        if (storage == null) {
            return Result.failure(IllegalStateException("Firebase Storage uninitialized. TODO: MANUAL CONFIGURATION REQUIRED"))
        }

        // Validate MIME type
        val normalizedMime = mimeType.lowercase()
        if (normalizedMime !in listOf("image/jpeg", "image/png", "image/webp")) {
            return Result.failure(IllegalArgumentException("Format tidak didukung. Gunakan JPEG, PNG, atau WEBP."))
        }

        // Validate size (max 5MB)
        if (imageBytes.size > 5 * 1024 * 1024) {
            return Result.failure(IllegalArgumentException("Ukuran gambar melebihi batas 5MB."))
        }

        val extension = when (normalizedMime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }

        val storagePath = "users/$uid/profile/avatar.$extension"
        val storageRef = storage.reference.child(storagePath)

        return try {
            val metadata = StorageMetadata.Builder()
                .setContentType(normalizedMime)
                .setCustomMetadata("uploadedBy", uid)
                .build()

            storageRef.putBytes(imageBytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
