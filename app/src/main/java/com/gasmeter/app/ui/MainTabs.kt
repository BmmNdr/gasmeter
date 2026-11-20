package com.gasmeter.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

enum class MainTab(val label: String) {
    TRIPS("Trips"),
    ROUTES("Routes"),
}

/** Bottom navigation between the two top-level screens. */
@Composable
fun MainTabs(current: MainTab, onSelect: (MainTab) -> Unit) {
    val extras = LocalGasMeterColors.current
    NavigationBar(containerColor = extras.card, tonalElevation = 0.dp) {
        MainTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == current,
                onClick = { if (tab != current) onSelect(tab) },
                icon = {
                    Icon(
                        if (tab == MainTab.TRIPS) Icons.AutoMirrored.Filled.List else Icons.Filled.Place,
                        contentDescription = null,
                    )
                },
                label = { Text(tab.label.uppercase(), style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = extras.muted,
                    unselectedTextColor = extras.muted,
                ),
            )
        }
    }
}
