package com.gasmeter.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gasmeter.app.ui.GasMeterTheme
import com.gasmeter.core.CarProfile
import com.gasmeter.core.TripCalculator
import com.gasmeter.core.TripMetrics
import com.gasmeter.core.TripReadings
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GasMeterTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    PlaceholderScreen(Modifier.padding(padding))
                }
            }
        }
    }
}

/** Temporary home screen until trip logging lands: shows the spec example via :core. */
@Composable
private fun PlaceholderScreen(modifier: Modifier = Modifier) {
    val example = TripCalculator.calculate(
        readings = TripReadings(
            rangeStartKm = 300.0,
            rangeEndKm = 295.0,
            odometerStartKm = 12_000.0,
            odometerEndKm = 12_002.0,
        ),
        car = CarProfile(consumptionLitresPer100Km = 6.0),
        fuelPricePerLitre = 1.80,
    )
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("GasMeter", style = MaterialTheme.typography.headlineMedium)
        Text("Trip logging is coming next. Example trip: 300 → 295 km range, 2 km driven.")
        ExampleCard(example)
    }
}

@Composable
private fun ExampleCard(m: TripMetrics) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Distance: ${m.distanceKm.fmt()} km")
            Text("Range used: ${m.rangeUsedKm.fmt()} km")
            Text("Wasted: ${m.excessKm.fmt()} km")
            Text("Ratio: ${m.ratio?.fmt() ?: "–"}×")
            Text("Cost: ${m.cost?.fmt(2) ?: "–"}")
        }
    }
}

private fun Double.fmt(decimals: Int = 1) = String.format(Locale.getDefault(), "%.${decimals}f", this)

@Preview(showBackground = true)
@Composable
private fun PlaceholderPreview() {
    GasMeterTheme { PlaceholderScreen() }
}
