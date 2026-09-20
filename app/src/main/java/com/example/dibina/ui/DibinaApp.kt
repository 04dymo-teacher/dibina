package com.example.dibina.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.dibina.data.AuthRepository
import com.example.dibina.data.FeedRepository
import com.example.dibina.data.JournalRepository
import com.example.dibina.data.LeaderboardRepository
import com.example.dibina.model.FeedItem
import com.example.dibina.model.JournalEntry
import com.example.dibina.model.LeaderboardEntry
import com.example.dibina.model.StudentProfile
import com.example.dibina.ui.navigation.BottomTab
import com.example.dibina.ui.navigation.DibinaBottomBar
import com.example.dibina.ui.navigation.NavScreen
import com.example.dibina.ui.screens.AchievementScreen
import com.example.dibina.ui.screens.AuthScreen
import com.example.dibina.ui.screens.HomeScreen
import com.example.dibina.ui.screens.JournalEntryScreen
import com.example.dibina.ui.screens.LeaderboardScreen
import com.example.dibina.ui.screens.RecapScreen
import com.example.dibina.ui.screens.SettingsScreen
import com.example.dibina.ui.screens.SplashScreen
import kotlinx.coroutines.launch

@Composable
fun DibinaApp(
    authRepository: AuthRepository = remember { AuthRepository() },
    journalRepository: JournalRepository = remember { JournalRepository() },
    feedRepository: FeedRepository = remember { FeedRepository() },
    leaderboardRepository: LeaderboardRepository = remember { LeaderboardRepository() }
) {
    var currentScreen by remember { mutableStateOf<NavScreen>(NavScreen.Splash) }
    var currentTab by remember { mutableStateOf<BottomTab>(BottomTab.Home) }
    var isJournalFormOpen by remember { mutableStateOf(false) }

    // Current Student Profile State (Defaults strictly: EXP=0, Level=1, Streak=0)
    var studentProfile by remember {
        mutableStateOf(
            StudentProfile(
                uid = "demo_student",
                name = "Siswa DIBINA",
                email = "siswa@sekolah.id",
                classId = "class_k6_26",
                classCode = "K6-26",
                totalExp = 0L,
                level = 1,
                currentStreak = 0,
                longestStreak = 0
            )
        )
    }

    // Observe realtime student profile from Firestore (users/{uid})
    val observedProfile by authRepository.observeStudentProfile(studentProfile.uid)
        .collectAsState(initial = null)
    val effectiveProfile = observedProfile ?: studentProfile

    // Observe today's journal for the student (to determine Create vs. Edit mode)
    val todayJournal by journalRepository.observeTodayJournal(
        uid = effectiveProfile.uid,
        classId = effectiveProfile.classId.orEmpty()
    ).collectAsState(initial = null)

    val todayHasJournal = todayJournal != null

    // Observe Feed, Leaderboard, and Journal History (Empty initially - NO DUMMY DATA)
    val feedItems by feedRepository.observeClassFeed(
        classId = effectiveProfile.classId.orEmpty()
    ).collectAsState(initial = emptyList())

    val leaderboardEntries by leaderboardRepository.observeClassLeaderboard(
        classId = effectiveProfile.classId.orEmpty()
    ).collectAsState(initial = emptyList())

    val journalHistory by journalRepository.observeStudentJournals(
        uid = effectiveProfile.uid
    ).collectAsState(initial = emptyList())

    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            NavScreen.Splash -> {
                SplashScreen(
                    onSplashFinished = {
                        val user = authRepository.currentFirebaseUser
                        if (user != null && !effectiveProfile.classId.isNullOrBlank()) {
                            currentScreen = NavScreen.Main
                        } else {
                            currentScreen = NavScreen.Auth
                        }
                    }
                )
            }

            NavScreen.Auth, NavScreen.JoinClass -> {
                AuthScreen(
                    authRepository = authRepository,
                    onAuthSuccess = { profile ->
                        studentProfile = profile
                        currentScreen = NavScreen.Main
                    }
                )
            }

            NavScreen.Achievement -> {
                AchievementScreen(
                    studentProfile = effectiveProfile,
                    onNavigateBack = { currentScreen = NavScreen.Main }
                )
            }

            NavScreen.Main -> {
                if (isJournalFormOpen) {
                    JournalEntryScreen(
                        studentProfile = effectiveProfile,
                        initialJournal = todayJournal,
                        onFetchJournalForDate = { dateStr ->
                            journalRepository.getJournalForDate(effectiveProfile.uid, dateStr)
                        },
                        onSaveJournal = { entry, shareToFeed ->
                            journalRepository.saveOrUpdateJournal(
                                entry = entry,
                                studentName = effectiveProfile.name,
                                profilePhotoUrl = effectiveProfile.profilePhotoUrl,
                                shareToFeed = shareToFeed
                            )
                        },
                        onShareToFeed = { entry ->
                            journalRepository.shareJournalToFeed(
                                entry = entry,
                                studentName = effectiveProfile.name,
                                profilePhotoUrl = effectiveProfile.profilePhotoUrl
                            )
                        },
                        onNavigateBack = {
                            isJournalFormOpen = false
                        }
                    )
                } else {
                    Scaffold(
                        bottomBar = {
                            DibinaBottomBar(
                                currentTab = currentTab,
                                todayHasJournal = todayHasJournal,
                                onTabSelected = { tab ->
                                    currentTab = tab
                                },
                                onCenterActionClicked = {
                                    // Opens Create mode if today's journal doesn't exist,
                                    // Opens Edit mode if today's journal exists
                                    isJournalFormOpen = true
                                }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                BottomTab.Home -> {
                                    HomeScreen(
                                        studentProfile = effectiveProfile,
                                        feedItems = feedItems,
                                        todayHasJournal = todayHasJournal,
                                        onOpenJournal = { isJournalFormOpen = true },
                                        onOpenAchievement = { currentScreen = NavScreen.Achievement }
                                    )
                                }

                                BottomTab.Recap -> {
                                    RecapScreen(
                                        journalHistory = journalHistory
                                    )
                                }

                                BottomTab.CenterAction -> {
                                    // Fallback if center tab is somehow activated directly
                                    isJournalFormOpen = true
                                }

                                BottomTab.Leaderboard -> {
                                    LeaderboardScreen(
                                        entries = leaderboardEntries,
                                        currentUid = effectiveProfile.uid
                                    )
                                }

                                BottomTab.Settings -> {
                                    SettingsScreen(
                                        studentProfile = effectiveProfile,
                                        onOpenAchievement = { currentScreen = NavScreen.Achievement },
                                        onSignOut = {
                                            authRepository.signOut()
                                            currentScreen = NavScreen.Auth
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
