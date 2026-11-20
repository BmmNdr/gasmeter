package com.gasmeter.app.ui.routes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gasmeter.app.ui.GasMeterScaffold
import com.gasmeter.app.ui.LocalGasMeterColors
import com.gasmeter.app.ui.MonoBody
import com.gasmeter.app.ui.SectionLabel
import com.gasmeter.app.ui.TagPill
import com.gasmeter.app.ui.formatRatio
import com.gasmeter.app.ui.shortLabel
import com.gasmeter.app.ui.tripCount
import com.gasmeter.app.ui.verdictColor

@Composable
fun RoutesScreen(
    viewModel: RoutesViewModel,
    onOpenRoute: (Long) -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val muted = LocalGasMeterColors.current.muted

    GasMeterScaffold(title = "Routes", bottomBar = bottomBar) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item { SectionLabel("Your routes · usual range per km") }

            if (!state.loading && state.routes.isEmpty()) {
                item {
                    Text(
                        "No routes yet. Routes are created when you start a trip: pick \"+ New route\" " +
                            "and give it a name like Home → Work.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            items(state.routes, key = { it.id }) { route ->
                RouteRow(route, onClick = { onOpenRoute(route.id) })
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun RouteRow(route: RouteListItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(route.name, style = MaterialTheme.typography.titleMedium)
            Text(
                tripCount(route.tripCount) + (route.medianRatio?.let { " · usual ${formatRatio(it)}" } ?: ""),
                style = MonoBody,
                color = LocalGasMeterColors.current.muted,
            )
        }
        route.lastVerdict.shortLabel()?.let { TagPill(it, color = verdictColor(route.lastVerdict)) }
    }
}
