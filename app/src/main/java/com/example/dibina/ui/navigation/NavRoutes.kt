package com.example.dibina.ui.navigation

sealed class NavScreen(val route: String) {
    object Splash : NavScreen("splash")
    object Auth : NavScreen("auth")
    object JoinClass : NavScreen("join_class")
    object Main : NavScreen("main")
    object Achievement : NavScreen("achievement")
}

sealed class BottomTab(val route: String, val titleResId: Int) {
    object Home : BottomTab("home", com.example.R.string.nav_home)
    object Recap : BottomTab("recap", com.example.R.string.nav_recap)
    object CenterAction : BottomTab("center_action", com.example.R.string.nav_create_journal)
    object Leaderboard : BottomTab("leaderboard", com.example.R.string.nav_leaderboard)
    object Settings : BottomTab("settings", com.example.R.string.nav_settings)
}
