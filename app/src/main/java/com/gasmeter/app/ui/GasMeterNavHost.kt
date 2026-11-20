package com.gasmeter.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gasmeter.app.data.TripRepository
import com.gasmeter.app.ui.end.EndTripScreen
import com.gasmeter.app.ui.end.EndTripViewModel
import com.gasmeter.app.ui.home.HomeScreen
import com.gasmeter.app.ui.home.HomeViewModel
import com.gasmeter.app.ui.route.RouteDetailScreen
import com.gasmeter.app.ui.route.RouteDetailViewModel
import com.gasmeter.app.ui.routes.RoutesScreen
import com.gasmeter.app.ui.routes.RoutesViewModel
import com.gasmeter.app.ui.settings.SettingsScreen
import com.gasmeter.app.ui.settings.SettingsViewModel
import com.gasmeter.app.ui.start.StartTripScreen
import com.gasmeter.app.ui.start.StartTripViewModel
import com.gasmeter.app.ui.trip.TripDetailScreen
import com.gasmeter.app.ui.trip.TripDetailViewModel

private object Destinations {
    const val HOME = "home"
    const val START = "start"
    const val END = "end"
    const val TRIP = "trip/{id}"
    const val ROUTES = "routes"
    const val ROUTE = "route/{id}"
    const val SETTINGS = "settings"
    fun trip(id: Long) = "trip/$id"
    fun route(id: Long) = "route/$id"
}

@Composable
fun GasMeterNavHost(repository: TripRepository) {
    val nav = rememberNavController()

    // Bottom tabs: keep Home at the bottom of the stack and restore each tab's state.
    val tabs: @Composable (MainTab) -> Unit = { current ->
        MainTabs(current) { tab ->
            val destination = if (tab == MainTab.TRIPS) Destinations.HOME else Destinations.ROUTES
            nav.navigate(destination) {
                popUpTo(Destinations.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    NavHost(navController = nav, startDestination = Destinations.HOME) {
        composable(Destinations.HOME) {
            HomeScreen(
                viewModel = viewModel { HomeViewModel(repository) },
                onStartTrip = { nav.navigate(Destinations.START) },
                onEndTrip = { nav.navigate(Destinations.END) },
                onOpenTrip = { id -> nav.navigate(Destinations.trip(id)) },
                bottomBar = { tabs(MainTab.TRIPS) },
                onOpenSettings = { nav.navigate(Destinations.SETTINGS) },
            )
        }
        composable(Destinations.ROUTES) {
            RoutesScreen(
                viewModel = viewModel { RoutesViewModel(repository) },
                onOpenRoute = { id -> nav.navigate(Destinations.route(id)) },
                bottomBar = { tabs(MainTab.ROUTES) },
                onOpenSettings = { nav.navigate(Destinations.SETTINGS) },
            )
        }
        composable(
            Destinations.ROUTE,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            RouteDetailScreen(
                viewModel = viewModel { RouteDetailViewModel(repository, id) },
                onBack = { nav.popBackStack() },
                onOpenTrip = { tripId -> nav.navigate(Destinations.trip(tripId)) },
            )
        }
        composable(Destinations.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel { SettingsViewModel(repository) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Destinations.START) {
            StartTripScreen(
                viewModel = viewModel { StartTripViewModel(repository) },
                onBack = { nav.popBackStack() },
                onStarted = { nav.popBackStack(Destinations.HOME, inclusive = false) },
            )
        }
        composable(Destinations.END) {
            EndTripScreen(
                viewModel = viewModel { EndTripViewModel(repository) },
                onBack = { nav.popBackStack() },
                onSaved = { id ->
                    nav.navigate(Destinations.trip(id)) {
                        popUpTo(Destinations.HOME)
                    }
                },
            )
        }
        composable(
            Destinations.TRIP,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            TripDetailScreen(
                viewModel = viewModel { TripDetailViewModel(repository, id) },
                onBack = { nav.popBackStack() },
                onOpenSettings = { nav.navigate(Destinations.SETTINGS) },
            )
        }
    }
}
