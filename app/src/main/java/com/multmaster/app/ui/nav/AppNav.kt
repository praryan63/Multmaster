package com.multmaster.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.multmaster.app.ui.LocalStrings
import com.multmaster.app.ui.home.HomeScreen
import com.multmaster.app.ui.learn.LearnDetailScreen
import com.multmaster.app.ui.learn.LearnScreen
import com.multmaster.app.ui.mental.MentalMathScreen
import com.multmaster.app.ui.mistakes.MistakesScreen
import com.multmaster.app.ui.onboarding.OnboardingScreen
import com.multmaster.app.ui.practice.PracticeSessionScreen
import com.multmaster.app.ui.practice.PracticeSetupScreen
import com.multmaster.app.ui.settings.SettingsScreen
import com.multmaster.app.ui.stats.StatsScreen

private val bottomRoutes = listOf("home", "learn", "practiceSetup", "mistakes", "stats")

@Composable
fun AppNav(startWithOnboarding: Boolean) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val t = LocalStrings.current

    Scaffold(
        bottomBar = {
            if (route in bottomRoutes) {
                NavigationBar {
                    listOf(
                        Triple("home", Icons.Filled.Home, t["home"]),
                        Triple("learn", Icons.Filled.MenuBook, t["learn"]),
                        Triple("practiceSetup", Icons.Filled.PlayArrow, t["practice"]),
                        Triple("mistakes", Icons.Filled.Bookmark, t["mistakes"]),
                        Triple("stats", Icons.Filled.BarChart, t["stats"])
                    ).forEach { (r, icon, label) ->
                        NavigationBarItem(
                            selected = route == r,
                            onClick = {
                                nav.navigate(r) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = if (startWithOnboarding) "onboarding" else "home",
            modifier = Modifier.padding(pad)
        ) {
            composable("onboarding") {
                OnboardingScreen(onDone = {
                    nav.navigate("home") { popUpTo("onboarding") { inclusive = true } }
                })
            }
            composable("home") { HomeScreen(nav) }
            composable("learn") { LearnScreen(nav) }
            composable(
                "learn/{table}",
                arguments = listOf(navArgument("table") { type = NavType.IntType })
            ) { entry ->
                LearnDetailScreen(entry.arguments?.getInt("table") ?: 1, nav)
            }
            composable("practiceSetup") { PracticeSetupScreen(nav) }
            composable("practice") { PracticeSessionScreen(onExit = { nav.popBackStack() }) }
            composable("mistakes") { MistakesScreen(nav) }
            composable("stats") { StatsScreen(nav) }
            composable("mental") { MentalMathScreen(nav) }
            composable("settings") { SettingsScreen(nav) }
        }
    }
}