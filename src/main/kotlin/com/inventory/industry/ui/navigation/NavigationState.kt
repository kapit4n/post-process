package com.inventory.industry.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class NavigationState(initialRoute: ScreenRoute = ScreenRoute.Dashboard) {
    var currentRoute: ScreenRoute by mutableStateOf(initialRoute)
        private set

    var sidebarCollapsed: Boolean by mutableStateOf(false)
        private set

    fun navigateTo(route: ScreenRoute) {
        currentRoute = route
    }

    fun updateSidebarCollapsed(collapsed: Boolean) {
        sidebarCollapsed = collapsed
    }

    fun toggleSidebarCollapsed() {
        sidebarCollapsed = !sidebarCollapsed
    }
}

@Composable
fun rememberNavigationState(initialRoute: ScreenRoute = ScreenRoute.Dashboard): NavigationState =
    remember { NavigationState(initialRoute) }
