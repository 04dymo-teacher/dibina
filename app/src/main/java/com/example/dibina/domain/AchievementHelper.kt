package com.example.dibina.domain

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.dibina.model.StudentProfile
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object AchievementHelper {

    /**
     * Generates a high-resolution, beautifully formatted JPG Bitmap of the student's achievement.
     * Contains:
     * - Profile Photo / Monogram
     * - Name
     * - Level
     * - EXP
     * - Streak
     * - Badge
     * - School
     * - Class
     *
     * Strictly EXCLUDES:
     * - Email
     * - NIS
     */
    fun createAchievementBitmap(
        context: Context,
        profile: StudentProfile,
        badge: BadgeTier
    ): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Background Gradient (White-Blue Theme)
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(Color.parseColor("#1E3A8A"), Color.parseColor("#2563EB"), Color.parseColor("#3B82F6")),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Soft decorative circle accents in background
        val accentPaint = Paint().apply {
            color = Color.WHITE
            alpha = 15
            isAntiAlias = true
        }
        canvas.drawCircle(width * 0.9f, height * 0.1f, 320f, accentPaint)
        canvas.drawCircle(width * 0.1f, height * 0.85f, 260f, accentPaint)

        // 2. Main White Card
        val cardMargin = 60f
        val cardRect = RectF(cardMargin, 90f, width - cardMargin, height - 90f)
        val cardPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            setShadowLayer(30f, 0f, 15f, Color.parseColor("#40000000"))
        }
        canvas.drawRoundRect(cardRect, 48f, 48f, cardPaint)

        // Top Card Accent Bar
        val topBarPaint = Paint().apply {
            color = Color.parseColor("#2563EB")
            isAntiAlias = true
        }
        val topBarRect = RectF(cardMargin, 90f, width - cardMargin, 110f)
        canvas.drawRoundRect(topBarRect, 20f, 20f, topBarPaint)

        // 3. Header Branding
        val brandPaint = Paint().apply {
            color = Color.parseColor("#1D4ED8")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("DIBINA • KEBANGGAAN ANAK INDONESIA", width / 2f, 180f, brandPaint)

        val subBrandPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 24f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("7 Kebiasaan Anak Indonesia Hebat", width / 2f, 220f, subBrandPaint)

        // Divider
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 3f
        }
        canvas.drawLine(cardMargin + 40f, 250f, width - cardMargin - 40f, 250f, dividerPaint)

        // 4. Avatar Monogram Circle
        val avatarCenterY = 370f
        val avatarRadius = 90f
        val avatarPaint = Paint().apply {
            color = Color.parseColor("#EFF6FF")
            isAntiAlias = true
        }
        canvas.drawCircle(width / 2f, avatarCenterY, avatarRadius, avatarPaint)

        val avatarBorderPaint = Paint().apply {
            color = Color.parseColor("#3B82F6")
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        canvas.drawCircle(width / 2f, avatarCenterY, avatarRadius, avatarBorderPaint)

        val initial = if (profile.name.isNotBlank()) profile.name.trim().take(1).uppercase() else "S"
        val initialPaint = Paint().apply {
            color = Color.parseColor("#1D4ED8")
            textSize = 72f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(initial, width / 2f, avatarCenterY + 26f, initialPaint)

        // 5. Student Name
        val namePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val displayName = if (profile.name.isNotBlank()) profile.name else "Siswa DIBINA"
        canvas.drawText(displayName, width / 2f, 510f, namePaint)

        // 6. School & Class (Strictly NO email, NO NIS)
        val schoolPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 26f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val schoolText = if (profile.schoolName.isNotBlank()) profile.schoolName else "Sekolah Penggerak"
        val classText = if (!profile.classCode.isNullOrBlank()) "Kelas ${profile.classCode}" else if (profile.grade.isNotBlank()) "Kelas ${profile.grade}" else "Kelas Siswa"
        canvas.drawText("$schoolText • $classText", width / 2f, 555f, schoolPaint)

        // 7. Badge Highlight Box
        val badgeBox = RectF(cardMargin + 50f, 600f, width - cardMargin - 50f, 780f)
        val badgeBoxPaint = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            isAntiAlias = true
        }
        canvas.drawRoundRect(badgeBox, 32f, 32f, badgeBoxPaint)

        val badgeBorder = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(badgeBox, 32f, 32f, badgeBorder)

        // Badge Symbol Emoji & Name
        val badgeSymbolPaint = Paint().apply {
            textSize = 58f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(badge.symbol, width / 2f, 675f, badgeSymbolPaint)

        val badgeNamePaint = Paint().apply {
            color = Color.parseColor("#1E3A8A")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Lencana: ${badge.nameId} (${badge.name})", width / 2f, 725f, badgeNamePaint)

        val badgeDescPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(badge.description, width / 2f, 758f, badgeDescPaint)

        // 8. Stats Metrics Grid (Level, Total EXP, Streak)
        val metricWidth = (width - 2 * cardMargin - 120f) / 3f
        val metricTop = 820f
        val metricHeight = 220f

        val metrics = listOf(
            Triple("LEVEL", "${profile.level}", "Tingkat"),
            Triple("TOTAL EXP", "${profile.totalExp}", "Poin Karakter"),
            Triple("STREAK", "${profile.currentStreak} Hari", "Konsistensi 🔥")
        )

        for (i in metrics.indices) {
            val (title, value, subtitle) = metrics[i]
            val left = cardMargin + 50f + i * (metricWidth + 10f)
            val rect = RectF(left, metricTop, left + metricWidth, metricTop + metricHeight)

            val statBgPaint = Paint().apply {
                color = when (i) {
                    0 -> Color.parseColor("#EFF6FF")
                    1 -> Color.parseColor("#FEF3C7")
                    else -> Color.parseColor("#FFF7ED")
                }
                isAntiAlias = true
            }
            canvas.drawRoundRect(rect, 24f, 24f, statBgPaint)

            val statTitlePaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(title, rect.centerX(), metricTop + 50f, statTitlePaint)

            val statValPaint = Paint().apply {
                color = when (i) {
                    0 -> Color.parseColor("#1D4ED8")
                    1 -> Color.parseColor("#B45309")
                    else -> Color.parseColor("#C2410C")
                }
                textSize = 38f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(value, rect.centerX(), metricTop + 120f, statValPaint)

            val statSubPaint = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 18f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(subtitle, rect.centerX(), metricTop + 170f, statSubPaint)
        }

        // 9. Habit Motivation Quote
        val quoteBox = RectF(cardMargin + 50f, 1070f, width - cardMargin - 50f, 1160f)
        val quoteBg = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            isAntiAlias = true
        }
        canvas.drawRoundRect(quoteBox, 20f, 20f, quoteBg)

        val quotePaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("“Membangun Kebiasaan Baik Hari Ini untuk Masa Depan Hebat”", width / 2f, 1125f, quotePaint)

        // 10. Footer Signature
        val footerPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("DIBINA • Aplikasi Pencatat 7 Kebiasaan Anak Indonesia Hebat", width / 2f, 1220f, footerPaint)

        return bitmap
    }

    /**
     * Saves the achievement Bitmap to the device's Pictures Gallery as a JPEG.
     */
    fun saveAchievementToGallery(context: Context, bitmap: Bitmap): Result<Uri> {
        return try {
            val filename = "DIBINA_Prestasi_${System.currentTimeMillis()}.jpg"
            var uri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DIBINA")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val dibinaDir = File(imagesDir, "DIBINA")
                if (!dibinaDir.exists()) {
                    dibinaDir.mkdirs()
                }
                val imageFile = File(dibinaDir, filename)
                FileOutputStream(imageFile).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                }
                uri = Uri.fromFile(imageFile)
            }

            if (uri != null) {
                Result.success(uri)
            } else {
                Result.failure(IllegalStateException("Gagal membuat berkas gambar di Galeri."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Shares the achievement JPEG image using the Android Sharesheet.
     * Uses:
     * - ACTION_SEND
     * - MIME: image/jpeg
     * - Does NOT hard-code WhatsApp or Instagram, uses compatible installed apps.
     */
    fun shareAchievement(context: Context, bitmap: Bitmap): Result<Unit> {
        return try {
            val cacheImagesDir = File(context.cacheDir, "images")
            if (!cacheImagesDir.exists()) {
                cacheImagesDir.mkdirs()
            }
            val shareFile = File(cacheImagesDir, "prestasi_dibina.jpg")
            val outputStream: OutputStream = FileOutputStream(shareFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            outputStream.flush()
            outputStream.close()

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, shareFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Lihat pencapaian 7 Kebiasaan Anak Indonesia Hebat saya di DIBINA! 🇮🇩✨ #DIBINA #GenerasiHebat"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Bagikan Prestasi DIBINA")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
