package com.personal.exercise.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.personal.exercise.data.ExerciseRepository
import com.personal.exercise.ui.home.HomeScreen
import com.personal.exercise.ui.metrics.MetricDetailScreen
import com.personal.exercise.ui.metrics.MetricsScreen
import com.personal.exercise.ui.plan.PlanScreen
import com.personal.exercise.ui.records.RecordsScreen
import com.personal.exercise.ui.settings.ActivitiesScreen
import com.personal.exercise.ui.settings.SettingsScreen
import com.personal.exercise.ui.theme.CardBg
import com.personal.exercise.ui.theme.Green
import com.personal.exercise.ui.theme.PageBg
import com.personal.exercise.ui.theme.TextHint
import com.personal.exercise.ui.theme.TextMain
import com.personal.exercise.ui.theme.TextMuted
import com.personal.exercise.ui.workout.WorkoutEditorScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val Tabs = listOf(
    Tab("home", "本周", Icons.Outlined.Home),
    Tab("records", "本月", Icons.AutoMirrored.Outlined.ListAlt),
    Tab("metrics", "指标", Icons.AutoMirrored.Outlined.ShowChart),
    Tab("settings", "设置", Icons.Outlined.Settings),
)

@Composable
fun ExerciseApp(repo: ExerciseRepository) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tabSelected = route in Tabs.map { it.route }
    val recordsVm: RecordsViewModel = viewModel(factory = VmFactory(repo))
    val selectedDay by recordsVm.selected.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = PageBg,
        floatingActionButton = {
            if (route == "records") {
                Surface(
                    onClick = { nav.navigate("workout/new/${selectedDay.toEpochDay()}") },
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = Green,
                    shadowElevation = 6.dp,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "添加",
                            modifier = Modifier.size(34.dp),
                            tint = Color.White,
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (tabSelected) {
                NavigationBar(containerColor = CardBg, contentColor = TextMain) {
                    Tabs.forEach { tab ->
                        val selected = route == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Green,
                                selectedTextColor = Green,
                                unselectedIconColor = TextHint,
                                unselectedTextColor = TextMuted,
                                indicatorColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("home") {
                HomeScreen(
                    vm = viewModel(factory = VmFactory(repo)),
                    onOpenPlan = { nav.navigate("plan") },
                )
            }
            composable("records") {
                RecordsScreen(
                    vm = recordsVm,
                    onOpen = { nav.navigate("workout/$it") },
                )
            }
            composable("plan") {
                PlanScreen(
                    vm = viewModel(factory = VmFactory(repo)),
                    onBack = { nav.popBackStack() },
                )
            }
            composable("metrics") {
                MetricsScreen(
                    vm = viewModel(factory = VmFactory(repo)),
                    onOpen = { nav.navigate("metric/$it") },
                )
            }
            composable("settings") {
                SettingsScreen(
                    vm = viewModel(factory = VmFactory(repo)),
                    onManageActivities = { nav.navigate("activities") },
                )
            }
            composable("activities") {
                ActivitiesScreen(
                    vm = viewModel(factory = VmFactory(repo)),
                    onBack = { nav.popBackStack() },
                )
            }
            composable("workout/new") {
                WorkoutEditorScreen(
                    vm = viewModel(factory = VmFactory(repo, null)),
                    onDone = { nav.popBackStack() },
                )
            }
            composable(
                route = "workout/new/{day}",
                arguments = listOf(navArgument("day") { type = NavType.LongType }),
            ) { entry ->
                val day = entry.arguments?.getLong("day")
                WorkoutEditorScreen(
                    vm = viewModel(factory = VmFactory(repo, null)),
                    initialDate = day?.let { java.time.LocalDate.ofEpochDay(it) },
                    onDone = { nav.popBackStack() },
                )
            }
            composable(
                route = "workout/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id")
                WorkoutEditorScreen(
                    vm = viewModel(factory = VmFactory(repo, id)),
                    onDone = { nav.popBackStack() },
                )
            }
            composable(
                route = "metric/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0
                MetricDetailScreen(
                    vm = viewModel(factory = VmFactory(repo, id)),
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
