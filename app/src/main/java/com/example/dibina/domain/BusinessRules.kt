package com.example.dibina.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object JournalIdHelper {
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    /**
     * Deterministic journal ID rule:
     * uid_YYYYMMDD
     * Example: abc123_20260920
     */
    fun generateJournalId(uid: String, dateString: String): String {
        require(uid.isNotBlank()) { "UID must not be blank" }
        require(dateString.matches(Regex("\\d{8}"))) { "Date must follow YYYYMMDD format" }
        return "${uid}_$dateString"
    }

    /**
     * Get current local date formatted as YYYYMMDD.
     */
    fun getTodayDateString(): String {
        return dateFormat.format(Date())
    }

    /**
     * Format YYYYMMDD into friendly Indonesian date (e.g. "20 September 2026").
     */
    fun formatFriendlyDate(dateString: String): String {
        return try {
            val date = dateFormat.parse(dateString) ?: Date()
            val friendlyFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
            friendlyFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    /**
     * Format YYYYMMDD into short date (e.g. "20 Sep").
     */
    fun formatShortDate(dateString: String): String {
        return try {
            val date = dateFormat.parse(dateString) ?: Date()
            val shortFormat = SimpleDateFormat("d MMM yyyy", Locale("id", "ID"))
            shortFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    /**
     * Generates selectable dates list:
     * Today + up to 7 days in the past.
     */
    fun getSelectablePastDates(): List<SelectableDate> {
        val list = mutableListOf<SelectableDate>()
        val cal = Calendar.getInstance()
        val todayStr = dateFormat.format(cal.time)

        for (i in 0..7) {
            val date = cal.time
            val dateStr = dateFormat.format(date)
            val label = when (i) {
                0 -> "Hari Ini (${formatShortDate(dateStr)})"
                1 -> "Kemarin (${formatShortDate(dateStr)})"
                else -> "${SimpleDateFormat("EEEE", Locale("id", "ID")).format(date)} (${formatShortDate(dateStr)})"
            }
            list.add(SelectableDate(dateString = dateStr, label = label, isToday = (i == 0)))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return list
    }

    /**
     * Validates if a date string is permitted:
     * - Cannot be in the future
     * - Cannot be older than 7 days in the past
     */
    fun validateJournalDate(dateString: String): DateValidationResult {
        if (!dateString.matches(Regex("\\d{8}"))) {
            return DateValidationResult.Invalid("Format tanggal harus YYYYMMDD.")
        }

        val targetDate = try {
            dateFormat.parse(dateString) ?: return DateValidationResult.Invalid("Format tanggal tidak valid.")
        } catch (e: Exception) {
            return DateValidationResult.Invalid("Format tanggal tidak valid.")
        }

        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val targetCal = Calendar.getInstance().apply {
            time = targetDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffMillis = todayCal.timeInMillis - targetCal.timeInMillis
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

        return when {
            diffDays < 0 -> DateValidationResult.Invalid("Tanggal jurnal tidak boleh di masa depan.")
            diffDays > 7 -> DateValidationResult.Invalid("Jurnal hanya dapat dicatat maksimal 7 hari ke belakang.")
            else -> DateValidationResult.Valid(isBackdate = diffDays > 0)
        }
    }

    /**
     * Formats shared habits highlights for Kabar Teman feed.
     * Generates a clear, inspiring summary of positive habits done.
     */
    fun formatSharedActivity(entry: com.example.dibina.model.JournalEntry): String {
        val habits = mutableListOf<String>()
        if (entry.bangunPagi || entry.bangunPagiTime.isNotBlank()) {
            val time = if (entry.bangunPagiTime.isNotBlank()) " (${entry.bangunPagiTime})" else ""
            habits.add("Bangun Pagi$time")
        }
        if (entry.ibadah || entry.ibadahSholat.isNotEmpty() || entry.ibadahDetails.isNotBlank()) {
            val count = entry.ibadahSholat.size
            val info = if (count > 0) " ($count Waktu)" else ""
            habits.add("Beribadah$info")
        }
        if (entry.olahraga || entry.olahragaActivity.isNotBlank()) {
            val act = if (entry.olahragaActivity.isNotBlank()) " (${entry.olahragaActivity})" else ""
            habits.add("Berolahraga$act")
        }
        val nutritionFilled = listOf(
            entry.karbohidrat || entry.karbohidratText.isNotBlank(),
            entry.protein || entry.proteinText.isNotBlank(),
            entry.lemak || entry.lemakText.isNotBlank(),
            entry.vitamin || entry.vitaminText.isNotBlank(),
            entry.serat || entry.seratText.isNotBlank(),
            entry.air || entry.airGelas >= 1
        ).count { it }
        if (nutritionFilled >= 3) {
            val water = if (entry.airGelas > 0) "${entry.airGelas} gelas air" else "Gizi Seimbang"
            habits.add("Makan Sehat ($water)")
        }
        if (entry.materiBelajar.isNotBlank() && entry.durasiBelajar >= 15) {
            habits.add("Gemar Belajar ${entry.materiBelajar} (${entry.durasiBelajar} mnt)")
        }
        if (entry.bermasyarakat || entry.bermasyarakatActivity.isNotBlank()) {
            val act = if (entry.bermasyarakatActivity.isNotBlank()) " (${entry.bermasyarakatActivity})" else ""
            habits.add("Bermasyarakat$act")
        }
        if (entry.tidurCepat || entry.tidurCepatTime.isNotBlank()) {
            val time = if (entry.tidurCepatTime.isNotBlank()) " (${entry.tidurCepatTime})" else ""
            habits.add("Tidur Cepat$time")
        }

        return if (habits.isNotEmpty()) {
            "Menjalankan ${habits.size} kebiasaan hebat: ${habits.joinToString(" • ")}"
        } else {
            "Telah mengisi jurnal 7 Kebiasaan Anak Indonesia Hebat hari ini."
        }
    }
}

data class SelectableDate(
    val dateString: String,
    val label: String,
    val isToday: Boolean
)

sealed class DateValidationResult {
    data class Valid(val isBackdate: Boolean) : DateValidationResult()
    data class Invalid(val reason: String) : DateValidationResult()
}

object ExpCalculator {
    const val EXP_PER_HABIT_MAX = 10
    const val MAX_DAILY_EXP = 70
    const val EXP_PER_LEVEL = 2000L

    /**
     * Estimated EXP for preview during editing.
     * Real authoritative final EXP is always calculated server-side in Cloud Functions.
     */
    fun calculateEstimatedExp(
        completedHabitsCount: Int,
        isBackdate: Boolean = false
    ): Int {
        val perHabit = if (isBackdate) 5 else EXP_PER_HABIT_MAX
        return (completedHabitsCount * perHabit).coerceAtMost(MAX_DAILY_EXP)
    }

    fun calculateExp(completedHabitsCount: Int): Int {
        return calculateEstimatedExp(completedHabitsCount, false)
    }

    /**
     * Calculate student level from total EXP.
     * Default: Level 1, EXP 0
     * Every 2,000 EXP: +1 level.
     * 0–1,999 = Level 1
     * 2,000–3,999 = Level 2
     * 4,000–5,999 = Level 3
     */
    fun calculateLevel(totalExp: Long): Int {
        return 1 + (totalExp.coerceAtLeast(0L) / EXP_PER_LEVEL).toInt()
    }

    /**
     * Progress percentage within current level (0.0 to 1.0).
     */
    fun levelProgress(totalExp: Long): Float {
        val currentExpInLevel = totalExp.coerceAtLeast(0L) % EXP_PER_LEVEL
        return (currentExpInLevel.toFloat() / EXP_PER_LEVEL.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Current EXP accumulated in current level.
     */
    fun expInCurrentLevel(totalExp: Long): Long {
        return totalExp.coerceAtLeast(0L) % EXP_PER_LEVEL
    }

    /**
     * EXP needed to reach the next level.
     */
    fun expNeededForNextLevel(totalExp: Long): Long {
        val currentInLevel = expInCurrentLevel(totalExp)
        return EXP_PER_LEVEL - currentInLevel
    }

    /**
     * Changes dashboard header color every multiple of 3 levels
     * while preserving the signature clean white-blue identity.
     */
    fun getDashboardHeaderColors(level: Int): HeaderThemeColors {
        val tier = (level / 3) % 4
        return when (tier) {
            1 -> HeaderThemeColors(
                primary = androidx.compose.ui.graphics.Color(0xFF1D4ED8), // Deep Royal Blue
                secondary = androidx.compose.ui.graphics.Color(0xFF2563EB),
                accent = androidx.compose.ui.graphics.Color(0xFFDBEAFE),
                title = "Tingkat Sapphire"
            )
            2 -> HeaderThemeColors(
                primary = androidx.compose.ui.graphics.Color(0xFF0369A1), // Ocean Cyan Blue
                secondary = androidx.compose.ui.graphics.Color(0xFF0284C7),
                accent = androidx.compose.ui.graphics.Color(0xFFE0F2FE),
                title = "Tingkat Samudra"
            )
            3 -> HeaderThemeColors(
                primary = androidx.compose.ui.graphics.Color(0xFF312E81), // Indigo Midnight Blue
                secondary = androidx.compose.ui.graphics.Color(0xFF4338CA),
                accent = androidx.compose.ui.graphics.Color(0xFFE0E7FF),
                title = "Tingkat Aurora"
            )
            else -> HeaderThemeColors(
                primary = androidx.compose.ui.graphics.Color(0xFF2563EB), // Classic Sky Blue
                secondary = androidx.compose.ui.graphics.Color(0xFF3B82F6),
                accent = androidx.compose.ui.graphics.Color(0xFFEFF6FF),
                title = "Tingkat Klasik"
            )
        }
    }
}

data class HeaderThemeColors(
    val primary: androidx.compose.ui.graphics.Color,
    val secondary: androidx.compose.ui.graphics.Color,
    val accent: androidx.compose.ui.graphics.Color,
    val title: String
)

/**
 * 13 Official Badges in DIBINA hierarchy.
 */
data class BadgeTier(
    val id: String,
    val name: String,
    val nameId: String,
    val minExp: Long,
    val minLevel: Int,
    val symbol: String,
    val description: String
)

object BadgeSystem {
    val ALL_BADGES = listOf(
        BadgeTier(
            id = "recruit",
            name = "Recruit",
            nameId = "Rekrut",
            minExp = 0L,
            minLevel = 1,
            symbol = "🛡️",
            description = "Awal langkah membiasakan 7 Kebiasaan Baik Anak Indonesia Hebat."
        ),
        BadgeTier(
            id = "private",
            name = "Private",
            nameId = "Prajurit",
            minExp = 2000L,
            minLevel = 2,
            symbol = "⚔️",
            description = "Mulai konsisten menjalankan ibadah, bangun pagi, dan belajar teratur."
        ),
        BadgeTier(
            id = "corporal",
            name = "Corporal",
            nameId = "Kopral",
            minExp = 4000L,
            minLevel = 3,
            symbol = "🎖️",
            description = "Semakin tangguh menjaga kebugaran tubuh dan pola makan sehat bergizi."
        ),
        BadgeTier(
            id = "sergeant",
            name = "Sergeant",
            nameId = "Sersan",
            minExp = 6000L,
            minLevel = 4,
            symbol = "🏅",
            description = "Menunjukkan kedisiplinan dan rasa tanggung jawab yang tinggi di kelas."
        ),
        BadgeTier(
            id = "veteran",
            name = "Veteran",
            nameId = "Veteran",
            minExp = 8000L,
            minLevel = 5,
            symbol = "🎗️",
            description = "Berpengalaman mempertahankan kebiasaan positif hari demi hari."
        ),
        BadgeTier(
            id = "elite",
            name = "Elite",
            nameId = "Elit",
            minExp = 10000L,
            minLevel = 6,
            symbol = "🌟",
            description = "Menjadi teladan kebaikan bagi teman-teman sebaya di lingkungan sekolah."
        ),
        BadgeTier(
            id = "captain",
            name = "Captain",
            nameId = "Kapten",
            minExp = 12000L,
            minLevel = 7,
            symbol = "⚡",
            description = "Memimpin dengan aksi nyata dalam gotong royong dan kepedulian sosial."
        ),
        BadgeTier(
            id = "commander",
            name = "Commander",
            nameId = "Komandan",
            minExp = 14000L,
            minLevel = 8,
            symbol = "🦅",
            description = "Komitmen kuat dalam menyeimbangkan belajar, ibadah, dan istirahat."
        ),
        BadgeTier(
            id = "warlord",
            name = "Warlord",
            nameId = "Panglima",
            minExp = 16000L,
            minLevel = 9,
            symbol = "👑",
            description = "Memiliki ketahanan mental luar biasa dalam menggapai prestasi terbaik."
        ),
        BadgeTier(
            id = "champion",
            name = "Champion",
            nameId = "Kampiun",
            minExp = 18000L,
            minLevel = 10,
            symbol = "🏆",
            description = "Juara sejati yang membuktikan konsistensi tanpa kenal lelah."
        ),
        BadgeTier(
            id = "legendary",
            name = "Legendary",
            nameId = "Legendaris",
            minExp = 20000L,
            minLevel = 11,
            symbol = "🔮",
            description = "Karakter mulia dan kebiasaan hebatnya menginspirasi seluruh sekolah."
        ),
        BadgeTier(
            id = "mythic",
            name = "Mythic",
            nameId = "Mitis",
            minExp = 22000L,
            minLevel = 12,
            symbol = "🪐",
            description = "Pencapaian luar biasa yang jarang dicapai, bukti integritas sejati."
        ),
        BadgeTier(
            id = "immortal",
            name = "Immortal",
            nameId = "Abadi",
            minExp = 24000L,
            minLevel = 13,
            symbol = "💎",
            description = "Tingkat tertinggi kebiasaan baik yang abadi dan melekat dalam jiwa."
        )
    )

    fun getActiveBadge(totalExp: Long, level: Int): BadgeTier {
        val unlocked = ALL_BADGES.filter { totalExp >= it.minExp && level >= it.minLevel }
        return unlocked.lastOrNull() ?: ALL_BADGES.first()
    }

    fun isBadgeUnlocked(badge: BadgeTier, totalExp: Long, level: Int): Boolean {
        return totalExp >= badge.minExp && level >= badge.minLevel
    }
}

object JournalConstants {
    // 5. Gemar Belajar - 15-minute increments only
    val STUDY_DURATION_OPTIONS = listOf(15, 30, 45, 60, 75, 90, 105, 120, 135, 150, 165, 180)

    // 4. Makan Sehat - Air minum selector
    val WATER_GLASS_OPTIONS = (1..12).toList()

    // 3. Berolahraga - Examples
    val EXERCISE_SUGGESTIONS = listOf(
        "Lari", "Senam", "Bersepeda", "Sepak bola", "Jalan kaki", "Bulu tangkis"
    )

    // 6. Bermasyarakat - Examples
    val COMMUNITY_SUGGESTIONS = listOf(
        "Menolong teman", "Membantu orang tua", "Peduli lingkungan",
        "Membantu guru", "Membersihkan lingkungan", "Berbagi dengan teman"
    )

    // 2. Beribadah - Muslim Sholat
    val MUSLIM_PRAYERS = listOf("Subuh", "Zuhur", "Asar", "Magrib", "Isya")

    // 2. Beribadah - Non-Muslim Devotions suggestions
    val NON_MUSLIM_DEVOTIONS = listOf(
        "Doa Pagi", "Kebaktian / Misa", "Membaca Kitab Suci", "Meditasi / Puja", "Doa Malam"
    )
}
