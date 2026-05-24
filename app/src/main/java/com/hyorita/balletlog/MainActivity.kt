package com.hyorita.balletlog

import android.os.Bundle
import coil.Coil
import coil.ImageLoader
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.hyorita.balletlog.data.HealthConnectManager
import androidx.compose.ui.res.stringResource
import com.hyorita.balletlog.R
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hyorita.balletlog.ui.history.HistoryScreen
import com.hyorita.balletlog.ui.home.ClassScreen
import com.hyorita.balletlog.ui.notes.NotesScreen
import com.hyorita.balletlog.ui.photolog.PhotoLogScreen
import com.hyorita.balletlog.ui.theme.BalletLogTheme

sealed class Screen(val route: String, val label: String, val icon: ImageVector, val labelRes: Int) {
    object Log : Screen("log", "Log", Icons.Default.PhotoLibrary, R.string.nav_log)
    object Class : Screen("class", "Class", Icons.Default.SportsGymnastics, R.string.nav_class)
    object Notes : Screen("notes", "Notes", Icons.Default.Note, R.string.nav_notes)
    object History : Screen("history", "History", Icons.Default.CalendarMonth, R.string.nav_history)
}

class MainActivity : ComponentActivity() {

    // Single source of truth lives in HealthConnectManager.permissions so the
    // request set never drifts from the read set used at fetch time.
    private val requestPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
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
            requestPermissions.launch(HealthConnectManager.permissions)
        }

        setContent {
            BalletLogTheme {
                BalletLogApp()
            }
        }
    }
}

/**
 * Lets nested screens (Editor, Detail, Settings) hide the root NavigationBar
 * while a full-screen overlay is presented. Without this, the overlay sits
 * inside NavHost's padded slot and the NavBar shows through underneath —
 * also double-counting its inset against IME padding.
 */
val LocalBottomBarVisible = compositionLocalOf { mutableStateOf(true) }

@Composable
fun BalletLogApp() {
    val navController = rememberNavController()
    val tabs = listOf(Screen.Log, Screen.Class, Screen.Notes, Screen.History)
    val bottomBarVisible = remember { mutableStateOf(true) }

    androidx.compose.runtime.CompositionLocalProvider(LocalBottomBarVisible provides bottomBarVisible) {
    Scaffold(
        // When a full-screen overlay is active, don't reserve the navigation
        // bar inset — otherwise children's IME padding stacks on top of it
        // and the chip bar floats one NavBar height above the keyboard.
        contentWindowInsets = if (bottomBarVisible.value)
            WindowInsets.systemBars
        else
            WindowInsets.systemBars.only(WindowInsetsSides.Top),
        bottomBar = {
            if (!bottomBarVisible.value) return@Scaffold
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
            // Apply BOTH top and bottom inner padding so screen-level bottom
            // bars (e.g. Notes' bottom-pinned search) sit above the NavBar.
            modifier = Modifier.padding(innerPadding),
            enterTransition = { androidx.compose.animation.EnterTransition.None },
            exitTransition = { androidx.compose.animation.ExitTransition.None }
        ) {
            composable(Screen.Log.route) { PhotoLogScreen() }
            composable(Screen.Class.route) { ClassScreen() }
            composable(Screen.Notes.route) { NotesScreen() }
            composable(Screen.History.route) { HistoryScreen() }
        }
    }
    }
}
