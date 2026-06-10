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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.platform.LocalContext
import com.hyorita.balletlog.data.BackfillPreferences
import com.hyorita.balletlog.data.HealthConnectAutoImport
import com.hyorita.balletlog.data.HealthConnectManager
import com.hyorita.balletlog.data.db.BalletLogDatabase
import com.hyorita.balletlog.util.debugLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    // 1.8 auto-PhotoLog throttle. Foreground events fire frequently
    // (notification pull-down, lock/unlock); we don't want to hit Health
    // Connect every time. 60s window mirrors iOS's "first foreground per
    // minute" cadence and is well under the 24h lookback so nothing slips.
    private var lastAutoImportAt: Long = 0L

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

    override fun onStart() {
        super.onStart()
        // 1.8: scan recent Health Connect workouts on foreground and insert
        // any new placeholders into Photo Log. Silent — the new rows just
        // appear in the grid via the existing Flow.
        val now = System.currentTimeMillis()
        if (now - lastAutoImportAt < 60_000L) return
        lastAutoImportAt = now
        lifecycleScope.launch {
            runCatching { HealthConnectAutoImport.importRecent(this@MainActivity) }
                .onFailure { debugLog("HealthConnect", "auto-import failed", it) }
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

    BackfillPrompt()
}

/**
 * 1.9 connect-time backfill. On the first launch with Health Connect granted,
 * scan the last 30 days for ballet workouts not yet in the log and offer to add
 * them all at once. Shown at most once (see [BackfillPreferences]); a launch
 * that finds nothing doesn't consume the offer.
 *
 * Unlike iOS — which needed a custom overlay because a SwiftUI .alert wouldn't
 * present near the TabView — a plain Material AlertDialog hosted at the app root
 * is enough on Android.
 */
@Composable
private fun BackfillPrompt() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember {
        mutableStateOf<List<HealthConnectManager.ScannedWorkout>>(emptyList())
    }
    // Stop re-checking once we've shown the prompt or confirmed (with permission
    // granted) there's nothing to offer this process.
    var handled by remember { mutableStateOf(false) }
    var inFlight by remember { mutableStateOf(false) }

    // Run on each ON_RESUME, not just first composition. Two cold-start races
    // would otherwise drop the one-time offer: a brand-new user grants Health
    // Connect via the system dialog *after* the app starts, and on a cold start
    // the HC client can briefly report "not granted" before warming up.
    // Resuming (returning from the dialog) re-checks, and a short poll absorbs
    // the warmup; the History banner remains the durable fallback regardless.
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME || handled || inFlight) return@LifecycleEventObserver
            inFlight = true
            scope.launch {
                try {
                    if (BackfillPreferences.hasPrompted(context)) { handled = true; return@launch }
                    if (!HealthConnectManager.isAvailable(context)) return@launch
                    // Tolerate cold-start warmup: poll briefly before giving up.
                    var granted = false
                    var tries = 0
                    while (tries < 8 && !granted) {
                        granted = HealthConnectManager.hasPermissions(context)
                        if (!granted) delay(500)
                        tries++
                    }
                    if (!granted) return@launch // not granted yet — retry next resume
                    val end = System.currentTimeMillis()
                    val start = end - 30L * 24 * 60 * 60 * 1000
                    val records = HealthConnectManager.scanWorkouts(context, start, end)
                    val dao = BalletLogDatabase.getInstance(context).photoLogDao()
                    val already = dao.getAllExternalWorkoutIds().toHashSet()
                    val fresh = records.filter { it.externalId !in already }
                    handled = true
                    if (fresh.isNotEmpty()) pending = fresh
                } finally {
                    inFlight = false
                }
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    if (pending.isEmpty()) return

    val records = pending
    val dismiss: () -> Unit = {
        BackfillPreferences.setPrompted(context)
        pending = emptyList()
    }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(R.string.backfill_title)) },
        text = { Text(stringResource(R.string.backfill_message, records.size)) },
        confirmButton = {
            TextButton(onClick = {
                BackfillPreferences.setPrompted(context)
                pending = emptyList()
                scope.launch {
                    val dao = BalletLogDatabase.getInstance(context).photoLogDao()
                    runCatching { HealthConnectAutoImport.importWorkouts(dao, records) }
                        .onFailure { debugLog("HealthConnect", "backfill failed", it) }
                }
            }) { Text(stringResource(R.string.backfill_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = dismiss) { Text(stringResource(R.string.backfill_dismiss)) }
        }
    )
}
