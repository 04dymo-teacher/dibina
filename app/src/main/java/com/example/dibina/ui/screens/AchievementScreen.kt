package com.example.dibina.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dibina.domain.AchievementHelper
import com.example.dibina.domain.BadgeSystem
import com.example.dibina.domain.ExpCalculator
import com.example.dibina.model.StudentProfile
import com.example.dibina.ui.components.DefaultAvatarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementScreen(
    studentProfile: StudentProfile,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val activeBadge = remember(studentProfile.totalExp, studentProfile.level) {
        BadgeSystem.getActiveBadge(studentProfile.totalExp, studentProfile.level)
    }

    var isGeneratingJpg by remember { mutableStateOf(false) }
    var isSavingToGallery by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Prestasi & Lencana",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF0F172A),
                    navigationIconContentColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFFF8FAFC),
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Digital Achievement Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("achievement_main_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header Badge ribbon
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEFF6FF))
                                .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "KARTU PRESTASI DIBINA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8),
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Profile Photo / Monogram
                        DefaultAvatarView(
                            photoUrl = studentProfile.photoUrl,
                            size = 80.dp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Student Name
                        Text(
                            text = studentProfile.name.ifBlank { "Siswa DIBINA" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )

                        // School & Class (STRICTLY NO email, NO NIS)
                        val schoolText = studentProfile.schoolName.ifBlank { "Sekolah Penggerak" }
                        val classText = if (!studentProfile.classCode.isNullOrBlank()) {
                            "Kelas ${studentProfile.classCode}"
                        } else if (studentProfile.grade.isNotBlank()) {
                            "Kelas ${studentProfile.grade}"
                        } else {
                            "Kelas Siswa"
                        }
                        Text(
                            text = "$schoolText • $classText",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Active Badge Highlight Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE))
                                    )
                                )
                                .border(1.dp, Color(0xFF93C5FD), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = activeBadge.symbol,
                                    fontSize = 44.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${activeBadge.nameId} (${activeBadge.name})",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF1E3A8A)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = activeBadge.description,
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Metric Trio: Level, EXP, Streak
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Level
                            StatBadgeBox(
                                title = "Level",
                                value = "${studentProfile.level}",
                                subtitle = "Tingkat",
                                containerColor = Color(0xFFEFF6FF),
                                textColor = Color(0xFF1D4ED8),
                                modifier = Modifier.weight(1f)
                            )

                            // EXP
                            StatBadgeBox(
                                title = "EXP",
                                value = "${studentProfile.totalExp}",
                                subtitle = "Total Poin",
                                containerColor = Color(0xFFFEF3C7),
                                textColor = Color(0xFFB45309),
                                modifier = Modifier.weight(1f)
                            )

                            // Streak
                            StatBadgeBox(
                                title = "Streak",
                                value = "${studentProfile.currentStreak}",
                                subtitle = "Hari 🔥",
                                containerColor = Color(0xFFFFF7ED),
                                textColor = Color(0xFFC2410C),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Level Progress to Next Level
                        val inLevel = ExpCalculator.expInCurrentLevel(studentProfile.totalExp)
                        val needed = ExpCalculator.expNeededForNextLevel(studentProfile.totalExp)
                        val progress = ExpCalculator.levelProgress(studentProfile.totalExp)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Progres ke Level ${studentProfile.level + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155)
                                )
                                Text(
                                    text = "$inLevel / 2.000 EXP",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF2563EB),
                                trackColor = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }

            // 2. Action Buttons: "Download JPG" and "Bagikan"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Download JPG Button
                    OutlinedButton(
                        onClick = {
                            if (isSavingToGallery) return@OutlinedButton
                            isSavingToGallery = true
                            coroutineScope.launch {
                                val bitmap = withContext(Dispatchers.Default) {
                                    AchievementHelper.createAchievementBitmap(
                                        context = context,
                                        profile = studentProfile,
                                        badge = activeBadge
                                    )
                                }
                                val result = withContext(Dispatchers.IO) {
                                    AchievementHelper.saveAchievementToGallery(context, bitmap)
                                }
                                isSavingToGallery = false
                                if (result.isSuccess) {
                                    Toast.makeText(
                                        context,
                                        "Prestasi berhasil disimpan ke Galeri Foto (Pictures/DIBINA)!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Gagal menyimpan ke Galeri: ${result.exceptionOrNull()?.localizedMessage}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("download_jpg_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB))
                    ) {
                        if (isSavingToGallery) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF2563EB)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Download JPG",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Bagikan Button (Android Sharesheet with ACTION_SEND and image/jpeg)
                    Button(
                        onClick = {
                            if (isGeneratingJpg) return@Button
                            isGeneratingJpg = true
                            coroutineScope.launch {
                                val bitmap = withContext(Dispatchers.Default) {
                                    AchievementHelper.createAchievementBitmap(
                                        context = context,
                                        profile = studentProfile,
                                        badge = activeBadge
                                    )
                                }
                                withContext(Dispatchers.IO) {
                                    AchievementHelper.shareAchievement(context, bitmap)
                                }
                                isGeneratingJpg = false
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("share_achievement_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        if (isGeneratingJpg) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bagikan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // 3. Badges Catalog (13 Badges: Locked remain visible as locked)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Katalog 13 Lencana DIBINA",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    val unlockedCount = BadgeSystem.ALL_BADGES.count {
                        BadgeSystem.isBadgeUnlocked(it, studentProfile.totalExp, studentProfile.level)
                    }
                    Text(
                        text = "$unlockedCount / 13 Terbuka",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB)
                    )
                }
            }

            items(BadgeSystem.ALL_BADGES) { badge ->
                val isUnlocked = BadgeSystem.isBadgeUnlocked(
                    badge = badge,
                    totalExp = studentProfile.totalExp,
                    level = studentProfile.level
                )
                val isCurrent = badge.id == activeBadge.id

                BadgeListItemCard(
                    badge = badge,
                    isUnlocked = isUnlocked,
                    isCurrent = isCurrent
                )
            }
        }
    }
}

@Composable
private fun StatBadgeBox(
    title: String,
    value: String,
    subtitle: String,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = textColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun BadgeListItemCard(
    badge: com.example.dibina.domain.BadgeTier,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("badge_item_${badge.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFFEFF6FF) else Color.White
        ),
        border = if (isCurrent) {
            androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF3B82F6))
        } else if (!isUnlocked) {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge Symbol Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    Text(
                        text = badge.symbol,
                        fontSize = 24.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Terkunci",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${badge.nameId} (${badge.name})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isUnlocked) Color(0xFF0F172A) else Color(0xFF94A3B8)
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2563EB))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Aktif",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = badge.description,
                    fontSize = 11.sp,
                    color = if (isUnlocked) Color(0xFF475569) else Color(0xFF94A3B8),
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isUnlocked) {
                        "Terbuka • Level ${badge.minLevel} (${badge.minExp} EXP)"
                    } else {
                        "Terkunci • Butuh Level ${badge.minLevel} (${badge.minExp} EXP)"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUnlocked) Color(0xFF16A34A) else Color(0xFF64748B)
                )
            }
        }
    }
}
