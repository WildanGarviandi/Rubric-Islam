package com.kellinreaver.rubricislam.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kellinreaver.rubricislam.ui.prayer.PrayerTimesScreen
import com.kellinreaver.rubricislam.ui.prayer.PrayerTimeViewModel
import com.kellinreaver.rubricislam.ui.qiblat.QiblatScreen
import com.kellinreaver.rubricislam.ui.qiblat.QiblatViewModel
import com.kellinreaver.rubricislam.ui.reminder.ReminderScreen
import com.kellinreaver.rubricislam.ui.reminder.ReminderViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.PrayerTimes.route) {
            val viewModel: PrayerTimeViewModel = hiltViewModel()
            PrayerTimesScreen(viewModel)
        }
        composable(Screen.Qiblat.route) {
            val viewModel: QiblatViewModel = hiltViewModel()
            QiblatScreen(viewModel)
        }
        composable(Screen.Reminders.route) {
            val viewModel: ReminderViewModel = hiltViewModel()
            ReminderScreen(viewModel)
        }
    }
}
