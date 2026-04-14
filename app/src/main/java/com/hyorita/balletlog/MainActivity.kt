package com.hyorita.balletlog

import android.os.Bundle
import coil.Coil
import coil.ImageLoader
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.compose.ui.res.stringResource
import com.hyorita.balletlog.R
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hyorita.balletlog.ui.history.HistoryScreen
import com.hyorita.balletlog.ui.home.HomeScreen
import com.hyorita.balletlog.ui.notes.NotesScreen
import com.hyorita.balletlog.ui.theme.BalletLogTheme

sealed class Screen(val route: String, val label: String, val icon: ImageVector, val labelRes: Int) {
    object Log : Screen("log", "Log", Icons.Default.SportsGymnastics, R.string.nav_log)
    object Notes : Screen("notes", "Notes", Icons.Default.Note, R.string.nav_notes)
    object History : Screen("history", "History", Icons.Default.CalendarMonth, R.string.nav_history)
}

class MainActivity : ComponentActivity() {

    private val healthPermissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    private val requestPermissions = registerForActivityResult(
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    ) { /* 결과는 hasPermissions()로 확인 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val imageLoader = ImageLoader.Builder(this)
            .allowHardware(false)
            .build()
        Coil.setImageLoader(imageLoader)

        // Health Connect 퍼미션 요청
        if (HealthConnectClient.getSdkStatus(this) == HealthConnectClient.SDK_AVAILABLE) {
            requestPermissions.launch(healthPermissions)
        }

        setContent {
            BalletLogTheme {
                BalletLogApp()
            }
        }
    }
}

@Composable
fun BalletLogApp() {
    val navController = rememberNavController()
    val tabs = listOf(Screen.Log, Screen.Notes, Screen.History)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                tabs.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = stringResource(screen.labelRes)) },
                        label = { Text(stringResource(screen.labelRes)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Log.route,
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            enterTransition = { androidx.compose.animation.EnterTransition.None },
            exitTransition = { androidx.compose.animation.ExitTransition.None }
        ) {
            composable(Screen.Log.route) { HomeScreen() }
            composable(Screen.Notes.route) { NotesScreen() }
            composable(Screen.History.route) { HistoryScreen() }
        }
    }
}
