package com.example.dibina.model

/**
 * Student profile representation in DIBINA.
 * Conforms to the exact Firestore schema:
 * users/{uid}
 *
 * Client MUST NOT directly modify:
 * role, classId, totalExp, level, currentStreak, longestStreak, createdAt, updatedAt.
 */
data class StudentProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val nis: String = "",
    val schoolName: String = "",
    val classId: String? = null,
    val classCode: String? = null,
    val grade: String = "",
    val religion: String = "",
    val profilePhotoUrl: String? = null,
    val totalExp: Long = 0L,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastRealtimeJournalDate: String? = null,
    val badgeId: String? = null,
    val badgeName: String? = null,
    val role: String = "student",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val photoUrl: String? get() = profilePhotoUrl
}

/**
 * Class representation in DIBINA.
 * Path: classes/{classId}
 */
data class ClassInfo(
    val classId: String = "",
    val classCode: String = "",
    val className: String = "",
    val grade: String = "",
    val schoolName: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val studentCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Class membership representation.
 * Path: classes/{classId}/members/{uid}
 */
data class ClassMember(
    val uid: String = "",
    val classId: String = "",
    val classCode: String = "",
    val studentName: String = "",
    val profilePhotoUrl: String? = null,
    val role: String = "student",
    val joinedAt: Long = System.currentTimeMillis()
)
