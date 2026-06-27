package com.kellinreaver.rubricislam.ui.navigation

sealed class Screen(val route: String) {
    object PrayerTimes : Screen("prayer_times")
    object Qiblat : Screen("qiblat")
    object Reminders : Screen("reminders")
}
