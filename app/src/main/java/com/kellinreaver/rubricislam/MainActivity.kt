package com.kellinreaver.rubricislam

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kellinreaver.rubricislam.ui.navigation.NavGraph
import com.kellinreaver.rubricislam.ui.navigation.Screen
import com.kellinreaver.rubricislam.ui.permissions.ExactAlarmPermissionRequestScreen
import com.kellinreaver.rubricislam.ui.permissions.LocationPermissionRequestScreen
import com.kellinreaver.rubricislam.ui.theme.RubricIslamTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.cos
import kotlin.math.sin

class RubElHizbShape : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): androidx.compose.ui.graphics.Outline {
        val path =
            Path().apply {
                val radius = size.minDimension / 2
                val centerX = size.width / 2
                val centerY = size.height / 2
                val sides = 8
                val innerRadius = radius * 0.7f

                for (i in 0 until sides * 2) {
                    val r = if (i % 2 == 0) radius else innerRadius
                    val angle = Math.toRadians((i * 360.0 / (sides * 2)) - 22.5)
                    val x = centerX + r * cos(angle).toFloat()
                    val y = centerY + r * sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
        return androidx.compose.ui.graphics.Outline
            .Generic(path)
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RubricIslamTheme {
                PermissionWrapper {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionWrapper(content: @Composable () -> Unit) {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    val permissionsState = rememberMultiplePermissionsState(permissions)
    var showAlarmPrompt by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val exactAlarmGranted =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(
                Context.ALARM_SERVICE
            ) as android.app.AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

    if (exactAlarmGranted) {
        showAlarmPrompt = false
    }

    if (!permissionsState.allPermissionsGranted) {
        LocationPermissionRequestScreen(
            shouldShowRationale = permissionsState.shouldShowRationale,
            onRequestPermission = {
                permissionsState.launchMultiplePermissionRequest()
            }
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        showAlarmPrompt &&
        !exactAlarmGranted
    ) {
        ExactAlarmPermissionRequestScreen(
            onAllow = { showAlarmPrompt = false },
            onSkip = { showAlarmPrompt = false }
        )
    } else {
        content()
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navItems =
        listOf(
            Screen.PrayerTimes,
            Screen.Qiblat,
            Screen.Reminders
        )
    val startDestination = Screen.PrayerTimes.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                navItems.forEach { screen ->
                    val selected =
                        currentDestination?.hierarchy?.any { it.route == screen.route } == true

                    NavigationBarItem(
                        icon = {
                            Box(contentAlignment = Alignment.Center) {
                                if (selected) {
                                    Box(
                                        modifier =
                                        Modifier
                                            .size(48.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.secondary.copy(
                                                    alpha = 0.2f
                                                ),
                                                shape = RubElHizbShape()
                                            )
                                    )
                                }
                                Icon(
                                    imageVector =
                                    if (selected) {
                                        when (screen) {
                                            Screen.PrayerTimes -> Icons.Filled.AccessTimeFilled
                                            Screen.Qiblat -> Icons.Filled.Explore
                                            Screen.Reminders -> Icons.Filled.Notifications
                                        }
                                    } else {
                                        when (screen) {
                                            Screen.PrayerTimes -> Icons.Outlined.AccessTime
                                            Screen.Qiblat -> Icons.Outlined.Explore
                                            Screen.Reminders -> Icons.Outlined.Notifications
                                        }
                                    },
                                    contentDescription = null,
                                    tint = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        },
                        label = {
                            Text(
                                text =
                                when (screen) {
                                    Screen.PrayerTimes -> stringResource(R.string.tab_prayer_name)
                                    Screen.Qiblat -> stringResource(R.string.tab_qiblat_name)
                                    Screen.Reminders -> stringResource(R.string.tab_alerts_name)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState =
                                            true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors =
                        NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavGraph(navController = navController, startDestination = startDestination)
        }
    }
}
