# GasMeter: product spec and build plan

## Context
You want an Android app that tracks how much fuel your everyday trips use, measured as **range-km**: how much the car's "range remaining" drops on a trip. If the range drops by more km than you actually drove, the trip cost more fuel than it should have. The app should also let you compare repeated runs of the same trip (average cost, trends and so on).

The repo (`BmmNdr/gasmeter`) is empty apart from a README. This plan turns the idea into an MVP spec plus a build order.

**Your decisions:**
- **Distance:** odometer reading at the start and end of each trip.
- **Range:** typed in by hand.
- **Same trip:** routes you name and save yourself.
- **Cost:** shown in km and in money.

## Weak points of the metric (the design has to handle these)
1. **Range is an estimate.** The car calculates it from recent consumption. It is usually rounded to 1, 5 or 10 km, it can go up while you drive (downhill, calm driving), and it jumps when you refuel.
2. **Short trips are noisy.** With a 5 km display step, a 2 km trip can show anything from 0 to 10 km used. One trip tells you little; **comparing repeated trips is the core feature**, so the app is built around it.
3. **Two numbers, not one.**
   - **Excess** = range used − distance. For your example, 5 − 2 = **3 km wasted**. This is the intuitive number.
   - **Ratio** = range used ÷ distance, which is **2.5×** in your example. This is the only number you can compare across trips of different lengths. 1.0× means the trip matched the car's own estimate.
4. **"Expensive" is relative to the route.** A short cold-engine city trip will always have a high ratio. The verdict compares each trip against **that route's own median**, not a fixed threshold.

## MVP features

### 1. Trip logging (two-step and fast)
- **Start trip:** pick a route (or "Unassigned"), then enter the **range** and the **odometer**. Both fields are prefilled from the last end reading, so you usually just confirm or adjust.
- **End trip:** enter the end range and the end odometer. The result card appears straight away.
- **Optional tags:** heavy traffic, AC/heating, cold engine, rain, load/passengers, highway. The time of day is recorded automatically. Tags help explain why one run was worse than another.
- **Persistent "trip in progress" notification/card** so you don't forget the end reading.

### 2. Result card for each trip
- Shows the distance, range used, excess km, ratio, fuel (L) and cost (€).
- **Verdict:**
  - **Efficient / Normal / Expensive:** given once the route has at least 3 *earlier* counted trips, so the first verdict appears on the route's 4th trip. Normal means within ±15% of the route's median ratio (bounds inclusive); Efficient is below that band and Expensive is above it.
  - **"Not enough history yet":** shown before then, together with the plain ratio.
  - **No verdict at all:** for idle trips (distance 0), trips where the range went up, refuelled or excluded trips, and routes whose median ratio is zero or negative.
- **Rounding hint:** "±X km" based on your display step setting. On very short trips it adds the note "too short to judge alone".

### 3. Saved routes and comparison (the core screen)
- **Route list:** each row shows the route name, number of trips, median ratio, average cost and the last trip's verdict.
- **Route detail:**
  - Count, median and average ratio, best and worst trip.
  - Average and total cost, plus total excess cost ("fuel you could have saved").
  - **Trend:** last 5 trips vs. all-time.
  - Chart of the ratio over time.
  - Breakdown by tag and time of day, e.g. "with heavy traffic: 1.8×, without: 1.2×".

### 4. Overview / dashboard
- This month's km driven, range-km used, total cost and excess cost.
- **Car calibration:** your overall ratio across all trips, i.e. how honest the car's range estimate is.

### 5. Settings
- **Car profile:** average consumption in L/100 km, and the range display step (1/5/10 km).
- **Fuel:** fuel price per litre, currency, and price history.
- **Data:** CSV export and import (backup).

## Calculations (pure logic, unit-tested)
- `distance = odoEnd − odoStart`
- `rangeUsed = rangeStart − rangeEnd`
- `excess = rangeUsed − distance`
- `ratio = rangeUsed / distance`
- `litres = rangeUsed × consumption / 100`
  - This works because the car's range ≈ fuel left ÷ recent consumption, so 1 range-km ≈ the fuel for 1 km at your average consumption.
- `cost = litres × fuelPrice`
  - Uses the fuel price that was valid **on the trip date**, so old trips stay accurate when prices change.
- `excessCost = excess × consumption / 100 × fuelPrice`
- **Verdict:**
  - `ratio / routeMedian(previous trips)` < 0.85 → Efficient
  - Between 0.85 and 1.15 → Normal
  - > 1.15 → Expensive
  - Requires at least 3 previous trips on the route.

## Edge cases
| Case | Handling |
|---|---|
| Refuel during a trip (range jumps up a lot) | Checkbox "refuelled during trip" → the trip is excluded from stats. A large unexplained increase triggers a prompt. |
| Range went up slightly (negative range used) | Allowed and stored; it is plausible on downhill or short trips. |
| End odometer ≤ start, or an implausible distance | Validation error or warning. |
| Forgotten end reading | The open trip stays pending. You can complete it later, discard it, or start a new trip (which asks about the old one first). |
| Distance 0 (car only idled) | No ratio. Shown as "X range-km used while idle". |
| Outliers | Medians are used rather than means, and you can exclude a trip from stats manually. |

## Tech stack (recommended)
- **Language and UI:** Kotlin with Jetpack Compose (Material 3).
- **Architecture:** single-activity MVVM with StateFlow.
- **Storage:** Room (SQLite), local only. There is no account or backend, and the app supports one car.
- **Charts:** Vico, a Compose chart library.
- **minSdk:** 26.

### Planned repo layout
- `app/`: Android UI, ViewModels, Room.
- `core/`: pure-Kotlin JVM module with the calculations, verdict logic and stats. It is unit-testable without Android.
- **Main entities:**
  - `Route(id, name, typicalDistance?)`
  - `Trip(id, routeId?, startTime, endTime, rangeStart, rangeEnd, odoStart, odoEnd, tags, refuelled, excluded, note)`
  - `FuelPrice(date, pricePerLitre)`
  - `CarSettings(consumption, rangeStep, currency)`

## Build order
1. Project scaffold (Gradle, `app` + `core`), plus the `core` calculations and verdict/stats logic with unit tests.
2. Room schema and repositories, then the start/end trip flow and the result card.
3. Routes: CRUD, the route detail stats screen and the chart.
4. Settings (car profile, fuel price history) and money conversion.
5. Dashboard, tags breakdown, CSV export/import, and the trip-in-progress notification.

## Later (not in MVP)
- OCR of the dashboard photo.
- Multiple cars.
- Automatic route detection by GPS.
- Refuel log (litres actually pumped), to cross-check the real consumption against the car's estimate.
- Home-screen widget for one-tap start/end.
- Cloud backup.

## Verification
- **What this environment has:** Java 21 and Gradle, but **no Android SDK**.
- **`core` unit tests:** they run here with `./gradlew :core:test`. They cover your 300→295 / 2 km example (excess 3, ratio 2.5), the verdict thresholds, negative deltas, refuel exclusion and median stats.
- **APK build:** needs the Android SDK command-line tools downloaded here (if the network allows it) or Android Studio on your machine. Then run `./gradlew assembleDebug`.
- **Real UI test:** install the debug APK on your phone and log a few real trips on one route. Check that the verdict first appears on the fourth trip and that the stats and costs match a hand calculation.
