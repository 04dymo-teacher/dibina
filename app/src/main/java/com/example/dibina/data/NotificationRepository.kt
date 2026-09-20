package com.example.dibina.data

import com.example.dibina.model.NotificationRecord
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for observing and managing user notifications.
 * Collection: notifications/{notificationId}
 */
class NotificationRepository(
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull(),
    private val messaging: FirebaseMessaging? = runCatching { FirebaseMessaging.getInstance() }.getOrNull()
) {

    /**
     * Observes notifications for the given student.
     * Query: notifications where targetUid == uid ordered by createdAt DESC.
     */
    fun observeNotifications(uid: String): Flow<List<NotificationRecord>> = callbackFlow {
        if (firestore == null || uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = firestore.collection("notifications")
            .whereEqualTo("targetUid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(NotificationRecord::class.java) } ?: emptyList()
            trySend(list)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Mark a notification as read.
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore uninitialized"))
        return try {
            db.collection("notifications").document(notificationId)
                .update("isRead", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch FCM registration token and save to users/{uid}.
     */
    suspend fun registerDeviceFcmToken(uid: String): Result<String> {
        val fcm = messaging ?: return Result.failure(IllegalStateException("FCM uninitialized. TODO: MANUAL CONFIGURATION REQUIRED"))
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore uninitialized"))
        return try {
            val token = fcm.token.await()
            db.collection("users").document(uid).update("fcmToken", token).await()
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
