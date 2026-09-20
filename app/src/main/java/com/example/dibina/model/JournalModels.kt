package com.example.dibina.model

/**
 * 7 Kebiasaan Anak Indonesia Hebat (7 KAIH).
 * Names remain in Bahasa Indonesia.
 */
enum class HabitType(
    val id: String,
    val title: String,
    val subtitle: String,
    val suggestedTime: String,
    val defaultExp: Int = 10
) {
    BANGUN_PAGI(
        id = "bangun_pagi",
        title = "Bangun Pagi",
        subtitle = "Bangun pagi pukul 04.00–05.00 dengan ceria dan mandiri",
        suggestedTime = "Pagi (04.00 - 05.00)"
    ),
    BERIBADAH(
        id = "beribadah",
        title = "Beribadah",
        subtitle = "Menjalankan ibadah dan doa sesuai agama & kepercayaan",
        suggestedTime = "Tepat Waktu"
    ),
    BEROLAHRAGA(
        id = "berolahraga",
        title = "Berolahraga",
        subtitle = "Senam, lari, jalan sehat, atau olahraga 15–30 menit",
        suggestedTime = "Pagi / Sore"
    ),
    MAKAN_SEHAT(
        id = "makan_sehat",
        title = "Makan Sehat dan Bergizi",
        subtitle = "Gizi seimbang: karbohidrat, protein, lemak, vitamin, serat, air",
        suggestedTime = "Sarapan & Siang"
    ),
    GEMAR_MEMBACA(
        id = "gemar_membaca",
        title = "Gemar Belajar",
        subtitle = "Belajar materi pengetahuan dan membaca buku",
        suggestedTime = "Siang / Malam"
    ),
    BERMASYARAKAT(
        id = "bermasyarakat",
        title = "Bermasyarakat",
        subtitle = "Membantu sesama, orang tua di rumah, atau teman di sekolah",
        suggestedTime = "Setiap Saat"
    ),
    TIDUR_TEPAT_WAKTU(
        id = "tidur_tepat_waktu",
        title = "Tidur Cepat",
        subtitle = "Istirahat dan tidur malam tepat waktu (rekomendasi: 19.30 - 21.00)",
        suggestedTime = "Malam (19.30 - 21.00)"
    );

    companion object {
        fun fromId(id: String): HabitType? = entries.find { it.id == id }
    }
}

/**
 * Journal Model according to exact Firestore schema:
 * Collection: journals/{journalId}
 * Deterministic ID: uid_YYYYMMDD
 *
 * CRITICAL CONSTRAINT:
 * There must be NO mineral field.
 */
data class JournalEntry(
    val journalId: String = "",
    val uid: String = "",
    val classId: String = "",
    val date: String = "", // Format: YYYYMMDD

    // 1. Bangun Pagi (Time Picker)
    val bangunPagi: Boolean = false,
    val bangunPagiTime: String = "", // e.g. "04:30"

    // 2. Beribadah (Religion specific)
    val ibadah: Boolean = false,
    val ibadahSholat: List<String> = emptyList(), // e.g. ["Subuh", "Zuhur", "Asar", "Magrib", "Isya"]
    val ibadahDetails: String = "", // For non-Muslim or additional devotions

    // 3. Berolahraga (Short text input + suggestions)
    val olahraga: Boolean = false,
    val olahragaActivity: String = "",

    // 4. Makan Sehat dan Bergizi (STRICTLY NO MINERAL FIELD!)
    val karbohidrat: Boolean = false,
    val karbohidratText: String = "",
    val protein: Boolean = false,
    val proteinText: String = "",
    val lemak: Boolean = false,
    val lemakText: String = "",
    val vitamin: Boolean = false,
    val vitaminText: String = "",
    val serat: Boolean = false,
    val seratText: String = "",
    val air: Boolean = false,
    val airGelas: Int = 0, // 1 to 12+ glasses

    // 5. Gemar Belajar (Materi + Waktu in 15-min increments)
    val materiBelajar: String = "",
    val durasiBelajar: Int = 0, // Multiples of 15 minutes: 15, 30, 45, 60, ...

    // 6. Bermasyarakat (Short text or recommended choices)
    val bermasyarakat: Boolean = false,
    val bermasyarakatActivity: String = "",

    // 7. Tidur Cepat (Time Picker)
    val tidurCepat: Boolean = false,
    val tidurCepatTime: String = "", // e.g. "20:00"

    val exp: Int = 0,
    val isBackdate: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val feedPostId: String? = null,
    val spreadsheetSyncStatus: String = "PENDING", // PENDING, SYNCED, ERROR
    val spreadsheetSyncedAt: Long? = null,
    val spreadsheetSyncError: String? = null
) {
    val habitCount: Int
        get() {
            var count = 0
            if (bangunPagi || bangunPagiTime.isNotBlank()) count++
            if (ibadah || ibadahSholat.isNotEmpty() || ibadahDetails.isNotBlank()) count++
            if (olahraga || olahragaActivity.isNotBlank()) count++

            val nutritionFilled = listOf(
                karbohidrat || karbohidratText.isNotBlank(),
                protein || proteinText.isNotBlank(),
                lemak || lemakText.isNotBlank(),
                vitamin || vitaminText.isNotBlank(),
                serat || seratText.isNotBlank(),
                air || airGelas >= 1
            ).count { it }
            if (nutritionFilled >= 3) count++

            if (materiBelajar.isNotBlank() && durasiBelajar >= 15) count++
            if (bermasyarakat || bermasyarakatActivity.isNotBlank()) count++
            if (tidurCepat || tidurCepatTime.isNotBlank()) count++
            return count
        }

    val completedCount: Int get() = habitCount
    val expEarned: Int get() = exp
    val journalReflection: String
        get() = when {
            materiBelajar.isNotBlank() -> "Belajar: $materiBelajar ($durasiBelajar menit)"
            bermasyarakatActivity.isNotBlank() -> "Kebaikan: $bermasyarakatActivity"
            olahragaActivity.isNotBlank() -> "Olahraga: $olahragaActivity"
            else -> ""
        }

    fun isHabitCompleted(type: HabitType): Boolean {
        return when (type) {
            HabitType.BANGUN_PAGI -> bangunPagi || bangunPagiTime.isNotBlank()
            HabitType.BERIBADAH -> ibadah || ibadahSholat.isNotEmpty() || ibadahDetails.isNotBlank()
            HabitType.BEROLAHRAGA -> olahraga || olahragaActivity.isNotBlank()
            HabitType.MAKAN_SEHAT -> {
                val nutritionFilled = listOf(
                    karbohidrat || karbohidratText.isNotBlank(),
                    protein || proteinText.isNotBlank(),
                    lemak || lemakText.isNotBlank(),
                    vitamin || vitaminText.isNotBlank(),
                    serat || seratText.isNotBlank(),
                    air || airGelas >= 1
                ).count { it }
                nutritionFilled >= 3
            }
            HabitType.GEMAR_MEMBACA -> materiBelajar.isNotBlank() && durasiBelajar >= 15
            HabitType.BERMASYARAKAT -> bermasyarakat || bermasyarakatActivity.isNotBlank()
            HabitType.TIDUR_TEPAT_WAKTU -> tidurCepat || tidurCepatTime.isNotBlank()
        }
    }
}

/**
 * Daily Stats Model according to exact Firestore schema:
 * Collection: dailyStats/{statId}
 * Deterministic ID: uid_YYYYMMDD (e.g. abc123_20260920)
 * One student + one date = one dailyStats document.
 */
data class DailyStat(
    val statId: String = "",
    val uid: String = "",
    val classId: String = "",
    val date: String = "", // Format: YYYYMMDD
    val habitsCompleted: Int = 0,
    val totalHabits: Int = 7,
    val expEarned: Int = 0,
    val isBackdate: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Feed Post Model according to exact Firestore schema:
 * Collection: feedPosts/{postId}
 */
data class FeedPost(
    val postId: String = "",
    val journalId: String = "",
    val uid: String = "",
    val classId: String = "",
    val studentName: String = "",
    val profilePhotoUrl: String? = null,
    val date: String = "",
    val summary: String = "",
    val materiBelajar: String? = null,
    val cheerCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val studentPhotoUrl: String? get() = profilePhotoUrl
    val completedCount: Int get() = 7
}

// Backward compatibility alias for UI
typealias FeedItem = FeedPost

/**
 * Leaderboard Entry for Class Leaderboard View
 */
data class LeaderboardEntry(
    val uid: String = "",
    val studentName: String = "",
    val profilePhotoUrl: String? = null,
    val studentPhotoUrl: String? = profilePhotoUrl,
    val totalExp: Long = 0L,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val rank: Int = 0
)

/**
 * Achievements per user:
 * Collection: achievements/{uid}
 */
data class AchievementRecord(
    val uid: String = "",
    val unlockedBadgeIds: List<String> = emptyList(),
    val totalBadgesCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Badge Definition:
 * Collection: badges/{badgeId}
 */
data class BadgeInfo(
    val badgeId: String = "",
    val title: String = "",
    val description: String = "",
    val icon: String = "",
    val category: String = "",
    val requiredExp: Long = 0L,
    val requiredStreak: Int = 0
)

/**
 * Notification Record:
 * Collection: notifications/{notificationId}
 */
data class NotificationRecord(
    val notificationId: String = "",
    val targetUid: String = "",
    val classId: String? = null,
    val title: String = "",
    val body: String = "",
    val type: String = "REMINDER", // REMINDER, FEED_CHEER, BADGE, STREAK
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Teacher Profile:
 * Collection: teachers/{uid}
 */
data class TeacherProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val schoolName: String = "",
    val assignedClassIds: List<String> = emptyList(),
    val role: String = "teacher",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Audit Log:
 * Collection: auditLogs/{logId}
 */
data class AuditLog(
    val logId: String = "",
    val actorUid: String = "",
    val action: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val details: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * App Configuration:
 * Collection: appConfig/{configId}
 */
data class AppConfig(
    val configId: String = "global",
    val minSupportedVersion: String = "1.0.0",
    val maintenanceMode: Boolean = false,
    val announcement: String? = null,
    val dailyReminderMorningHour: Int = 5,
    val dailyReminderEveningHour: Int = 21
)
