package com.example.dibina.data

import com.example.dibina.model.FeedPost
import com.example.dibina.model.LeaderboardEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Repository for observing class feed from top-level feedPosts/{postId}.
 */
class FeedRepository(
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
) {
    /**
     * Observes real-time feed of class journals from feedPosts collection.
     * Filtered by classId, ordered by createdAt DESC.
     */
    fun observeClassFeed(classId: String): Flow<List<FeedPost>> = callbackFlow {
        if (firestore == null || classId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = firestore.collection("feedPosts")
            .whereEqualTo("classId", classId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val posts = snapshot?.documents?.mapNotNull { it.toObject(FeedPost::class.java) } ?: emptyList()
            trySend(posts)
        }

        awaitClose { listener.remove() }
    }
}

/**
 * Repository for class leaderboard from users collection.
 * Query: users where classId == classId ordered by totalExp DESC.
 */
class LeaderboardRepository(
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
) {
    /**
     * Observes class leaderboard sorted by total EXP.
     */
    fun observeClassLeaderboard(classId: String): Flow<List<LeaderboardEntry>> = callbackFlow {
        if (firestore == null || classId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = firestore.collection("users")
            .whereEqualTo("classId", classId)
            .orderBy("totalExp", Query.Direction.DESCENDING)
            .limit(50)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            var rank = 1
            val entries = snapshot?.documents?.mapNotNull { doc ->
                val name = doc.getString("name") ?: "Siswa"
                val photoUrl = doc.getString("profilePhotoUrl") ?: doc.getString("photoUrl")
                val totalExp = doc.getLong("totalExp") ?: 0L
                val level = doc.getLong("level")?.toInt() ?: 1
                val streak = doc.getLong("currentStreak")?.toInt() ?: 0
                val uid = doc.getString("uid") ?: doc.id

                LeaderboardEntry(
                    uid = uid,
                    studentName = name,
                    studentPhotoUrl = photoUrl,
                    totalExp = totalExp,
                    level = level,
                    currentStreak = streak,
                    rank = rank++
                )
            } ?: emptyList()

            trySend(entries)
        }

        awaitClose { listener.remove() }
    }
}
