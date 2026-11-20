package com.gasmeter.app

import android.app.Application
import com.gasmeter.app.data.GasMeterDatabase
import com.gasmeter.app.data.TripRepository

class GasMeterApp : Application() {
    val database: GasMeterDatabase by lazy { GasMeterDatabase.create(this) }

    val repository: TripRepository by lazy { TripRepository(database) }
}
