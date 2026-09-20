package com.example.dibina.data

import com.example.dibina.domain.DateValidationResult
import com.example.dibina.domain.ExpCalculator
import com.example.dibina.domain.JournalIdHelper
import com.example.dibina.model.DailyStat
import com.example.dibina.model.FeedPost
import com.example.dibina.model.JournalEntry
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Journal Repository managing journals, daily stats, and class feeds.
 *
 * Rules:
 * - ONE STUDENT + ONE DATE = EXACTLY ONE JOURNAL.
 * - Deterministic journalId: uid_YYYYMMDD.
 * - Allowed dates: Today and up to 7 days in the past.
 * - EDIT = UPDATE / REPLACE (Does not duplicate EXP, does not create new feed post).
 * - CREATE = INSERT.
 * - Strictly NO mineral field.
 */
class JournalRepository(
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
) {

    /**
     * Fetch a journal entry for a specific student and date.
     * Document ID is deterministic: "${uid}_${dateString}".
     */
    suspend fun getJournalForDate(uid: String, dateString: String): JournalEntry? {
        if (firestore == null || uid.isBlank() || dateString.isBlank()) return null
        return try {
            val docId = JournalIdHelper.generateJournalId(uid, dateString)
            val doc = firestore.collection("journals").document(docId).get().await()
            if (doc.exists()) doc.toObject(JournalEntry::class.java) else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Observes real-time journal entry for a specific date.
     */
    fun observeJournalForDate(uid: String, dateString: String): Flow<JournalEntry?> = callbackFlow {
        if (firestore == null || uid.isBlank() || dateString.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val docId = JournalIdHelper.generateJournalId(uid, dateString)
        val docRef = firestore.collection("journals").document(docId)

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toObject(JournalEntry::class.java))
            } else {
                trySend(null)
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Observes today's journal for a student.
     */
    fun observeTodayJournal(uid: String, classId: String = ""): Flow<JournalEntry?> {
        val today = JournalIdHelper.getTodayDateString()
        return observeJournalForDate(uid, today)
    }

    /**
     * Synchronously checks if today's journal exists.
     */
    suspend fun checkTodayJournalExists(uid: String): JournalEntry? {
        val today = JournalIdHelper.getTodayDateString()
        return getJournalForDate(uid, today)
    }

    /**
     * Saves or updates a journal entry atomically using transactions.
     *
     * Rules enforced:
     * - Date validation: Today or up to 7 days in the past only. Future dates rejected.
     * - Deterministic ID: uid_YYYYMMDD.
     * - If editing: updates existing document, does not duplicate EXP (only awards diff if higher).
     * - If feed post already exists for this journal, updates existing feed post; never creates another feed post.
     * - If shareToFeed is selected, creates exactly one feed post.
     * - Stored feed fields: journalId, uid, classId, name, profilePhotoUrl, date, sharedActivity, createdAt, updatedAt.
     */
    suspend fun saveOrUpdateJournal(
        entry: JournalEntry,
        studentName: String = "",
        profilePhotoUrl: String? = null,
        shareToFeed: Boolean = false
    ): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firebase Firestore tidak terhubung."))

        // 1. Date Validation: Today or up to 7 days in the past
        val dateValidation = JournalIdHelper.validateJournalDate(entry.date)
        if (dateValidation is DateValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(dateValidation.reason))
        }
        val isBackdate = (dateValidation as DateValidationResult.Valid).isBackdate

        // 2. Multiples of 15 validation for study duration
        if (entry.durasiBelajar > 0 && entry.durasiBelajar % 15 != 0) {
            return Result.failure(IllegalArgumentException("Durasi belajar harus dalam kelipatan 15 menit."))
        }

        val deterministicId = JournalIdHelper.generateJournalId(entry.uid, entry.date)
        val completedHabitsCount = entry.habitCount
        val estimatedExp = ExpCalculator.calculateEstimatedExp(completedHabitsCount, isBackdate)

        val journalDocRef = db.collection("journals").document(deterministicId)
        val dailyStatDocRef = db.collection("dailyStats").document(deterministicId)
        val feedPostDocRef = db.collection("feedPosts").document(deterministicId)
        val userDocRef = db.collection("users").document(entry.uid)

        return try {
            db.runTransaction { transaction ->
                val existingJournalDoc = transaction.get(journalDocRef)
                val isEditMode = existingJournalDoc.exists()
                val existingFeedDoc = transaction.get(feedPostDocRef)
                val feedAlreadyExists = existingFeedDoc.exists()
                val shouldUpdateOrPostFeed = shareToFeed || entry.sharedToFeed || feedAlreadyExists

                val now = System.currentTimeMillis()

                val finalJournal = entry.copy(
                    journalId = deterministicId,
                    exp = estimatedExp, // Initial placeholder, final authoritative EXP is calculated server-side
                    isBackdate = isBackdate,
                    feedPostId = if (shouldUpdateOrPostFeed) deterministicId else existingJournalDoc.getString("feedPostId"),
                    sharedToFeed = shouldUpdateOrPostFeed,
                    spreadsheetSyncStatus = "PENDING",
                    createdAt = if (isEditMode) (existingJournalDoc.getLong("createdAt") ?: now) else now,
                    updatedAt = now
                )

                val dailyStat = DailyStat(
                    statId = deterministicId,
                    uid = entry.uid,
                    classId = entry.classId,
                    date = entry.date,
                    habitsCompleted = completedHabitsCount,
                    totalHabits = 7,
                    expEarned = estimatedExp,
                    isBackdate = isBackdate,
                    createdAt = finalJournal.createdAt,
                    updatedAt = now
                )

                // Write Journal & DailyStat
                transaction.set(journalDocRef, finalJournal, SetOptions.merge())
                transaction.set(dailyStatDocRef, dailyStat, SetOptions.merge())

                // Feed Post Management:
                // If feed already exists -> UPDATE existing feed post.
                // If user selected "Bagikan ke Kabar Teman" -> CREATE exactly one feed post.
                // Each journal has maximum one feed post with ID == deterministicId.
                if (shouldUpdateOrPostFeed) {
                    val sharedActivitySummary = JournalIdHelper.formatSharedActivity(entry)
                    val feedPost = FeedPost(
                        postId = deterministicId,
                        journalId = deterministicId,
                        uid = entry.uid,
                        classId = entry.classId,
                        name = if (studentName.isNotBlank()) studentName else "Siswa DIBINA",
                        profilePhotoUrl = profilePhotoUrl,
                        date = entry.date,
                        sharedActivity = sharedActivitySummary,
                        createdAt = if (feedAlreadyExists) (existingFeedDoc.getLong("createdAt") ?: now) else now,
                        updatedAt = now
                    )
                    transaction.set(feedPostDocRef, feedPost, SetOptions.merge())
                }

                // Touch user document updatedAt
                val userDoc = transaction.get(userDocRef)
                if (userDoc.exists()) {
                    transaction.update(userDocRef, mapOf<String, Any>(
                        "updatedAt" to now
                    ))
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Explicit "Bagikan ke Kabar Teman" flow after journal submission.
     * Ensures exactly one feed post per journal.
     * If post already exists, updates it.
     */
    suspend fun shareJournalToFeed(
        entry: JournalEntry,
        studentName: String,
        profilePhotoUrl: String?
    ): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firebase Firestore tidak terhubung."))
        val deterministicId = JournalIdHelper.generateJournalId(entry.uid, entry.date)
        val feedPostDocRef = db.collection("feedPosts").document(deterministicId)
        val journalDocRef = db.collection("journals").document(deterministicId)

        return try {
            val now = System.currentTimeMillis()
            val sharedActivity = JournalIdHelper.formatSharedActivity(entry)

            val existingDoc = feedPostDocRef.get().await()
            val createdAt = if (existingDoc.exists()) (existingDoc.getLong("createdAt") ?: now) else now

            val feedPost = FeedPost(
                postId = deterministicId,
                journalId = deterministicId,
                uid = entry.uid,
                classId = entry.classId,
                name = if (studentName.isNotBlank()) studentName else "Siswa DIBINA",
                profilePhotoUrl = profilePhotoUrl,
                date = entry.date,
                sharedActivity = sharedActivity,
                createdAt = createdAt,
                updatedAt = now
            )

            feedPostDocRef.set(feedPost, SetOptions.merge()).await()
            journalDocRef.update(
                mapOf<String, Any>(
                    "sharedToFeed" to true,
                    "feedPostId" to deterministicId,
                    "updatedAt" to now
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get journal history for a student.
     */
    fun observeStudentJournals(uid: String): Flow<List<JournalEntry>> = callbackFlow {
        if (firestore == null || uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = firestore.collection("journals")
            .whereEqualTo("uid", uid)
            .orderBy("date", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(JournalEntry::class.java) } ?: emptyList()
            trySend(list)
        }

        awaitClose { listener.remove() }
    }
}
