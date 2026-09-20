package com.example.dibina.data

import com.example.dibina.model.ClassInfo
import com.example.dibina.model.ClassMember
import com.example.dibina.model.StudentProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Authentication and User Profile Repository for DIBINA.
 *
 * Enforces client constraints:
 * Client MUST NOT directly modify:
 * - role
 * - classId
 * - totalExp
 * - level
 * - currentStreak
 * - longestStreak
 * - createdAt
 * - updatedAt
 */
class AuthRepository(
    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull(),
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
) {

    /**
     * Check if user is currently signed in.
     */
    val currentFirebaseUser: FirebaseUser?
        get() = auth?.currentUser

    /**
     * Observe current student profile from Firestore users/{uid}.
     */
    fun observeStudentProfile(uid: String): Flow<StudentProfile?> = callbackFlow {
        if (firestore == null || uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(StudentProfile::class.java)
                    trySend(profile)
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Initialize student profile after Google Sign-In.
     * Default values:
     * - role = "student"
     * - totalExp = 0, level = 1, currentStreak = 0, longestStreak = 0
     */
    suspend fun initializeUserProfile(user: FirebaseUser): Result<StudentProfile> {
        return try {
            val uid = user.uid
            val email = user.email ?: ""
            val resolvedName = when {
                !user.displayName.isNullOrBlank() -> user.displayName!!
                email.contains("@") -> email.substringBefore("@")
                else -> "Siswa DIBINA"
            }
            val photoUrl = user.photoUrl?.toString()

            val db = firestore ?: throw IllegalStateException("Firebase Firestore not initialized. TODO: MANUAL CONFIGURATION REQUIRED")
            val userDocRef = db.collection("users").document(uid)
            val existingDoc = userDocRef.get().await()

            val profile = if (existingDoc.exists()) {
                val existing = existingDoc.toObject(StudentProfile::class.java) ?: StudentProfile()
                existing.copy(
                    name = resolvedName,
                    email = email,
                    profilePhotoUrl = photoUrl ?: existing.profilePhotoUrl
                )
            } else {
                StudentProfile(
                    uid = uid,
                    name = resolvedName,
                    email = email,
                    nis = "",
                    schoolName = "",
                    classId = null,
                    classCode = null,
                    grade = "",
                    religion = "",
                    profilePhotoUrl = photoUrl,
                    totalExp = 0L,
                    level = 1,
                    currentStreak = 0,
                    longestStreak = 0,
                    lastRealtimeJournalDate = null,
                    role = "student",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }

            if (!existingDoc.exists()) {
                userDocRef.set(profile).await()
            } else {
                // Client only updates safe editable fields during re-auth: name, email, profilePhotoUrl
                userDocRef.update(
                    mapOf(
                        "name" to resolvedName,
                        "email" to email,
                        "profilePhotoUrl" to (photoUrl ?: existingDoc.getString("profilePhotoUrl")),
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            }

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update client-editable profile fields only.
     * Enforces the constraint: Client MUST NOT modify role, classId, totalExp, level, streaks.
     */
    suspend fun updateStudentEditableProfile(
        uid: String,
        name: String,
        nis: String,
        schoolName: String,
        grade: String,
        religion: String,
        profilePhotoUrl: String?
    ): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore uninitialized"))
        return try {
            val updates = mutableMapOf<String, Any>(
                "name" to name,
                "nis" to nis,
                "schoolName" to schoolName,
                "grade" to grade,
                "religion" to religion,
                "updatedAt" to System.currentTimeMillis()
            )
            if (profilePhotoUrl != null) {
                updates["profilePhotoUrl"] = profilePhotoUrl
            }

            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Join class through classCode -> classId.
     * Resolves and validates class membership server-side.
     * Updates classes/{classId}/members/{uid} and users/{uid}.
     */
    suspend fun joinClassWithCode(uid: String, rawCode: String, studentName: String = ""): Result<ClassInfo> {
        val trimmedCode = rawCode.trim().uppercase()
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore uninitialized"))

        return try {
            val snapshot = db.collection("classes")
                .whereEqualTo("classCode", trimmedCode)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                return Result.failure(IllegalArgumentException("Kode kelas '$trimmedCode' tidak ditemukan."))
            }

            val classDoc = snapshot.documents.first()
            val classId = classDoc.id
            val classInfo = classDoc.toObject(ClassInfo::class.java)
                ?: ClassInfo(classId = classId, classCode = trimmedCode, className = "Kelas $trimmedCode")

            // Atomic batch update for membership
            val batch = db.batch()

            // 1. users/{uid} update classId & classCode
            val userRef = db.collection("users").document(uid)
            batch.update(
                userRef,
                mapOf(
                    "classId" to classId,
                    "classCode" to trimmedCode,
                    "updatedAt" to System.currentTimeMillis()
                )
            )

            // 2. classes/{classId}/members/{uid}
            val memberRef = db.collection("classes").document(classId)
                .collection("members").document(uid)
            val member = ClassMember(
                uid = uid,
                classId = classId,
                classCode = trimmedCode,
                studentName = studentName,
                role = "student",
                joinedAt = System.currentTimeMillis()
            )
            batch.set(memberRef, member, SetOptions.merge())

            // 3. Increment class studentCount
            batch.update(classDoc.reference, "studentCount", FieldValue.increment(1))

            batch.commit().await()

            Result.success(classInfo.copy(classId = classId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign out current student.
     */
    fun signOut() {
        auth?.signOut()
    }
}
