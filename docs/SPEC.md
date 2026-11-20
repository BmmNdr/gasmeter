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

## Thresholds chosen in code (tune after real use)
- **Too short to judge:** distance under 2 range display steps (under 10 km with a 5 km step).
- **Refuel prompt:** range goes up by more than 30 km, or 3 display steps if that is larger.
- **Implausible distance warning:** over 1500 km in one trip.
- **Trend:** median of the last 5 trips vs. all trips; within ±5% counts as stable. Needs more than 5 trips.
- **Range went up (negative ratio):** kept in medians, where rounding errors cancel out, but never shown as best/worst trip and never given a verdict.

## Decisions made while building step 2 (trip logging)
- **Number input:** both `12.5` and `12,5` work. With both `.` and `,` present, the last one is the decimal separator. A separator that appears more than once is a thousands separator (`1.234.567`). A single separator is always a decimal, so `12.345` means 12.345 km, not 12345.
- **Range display step:** asked once, on the first Start trip. It can't be changed until the settings screen exists (step 4).
- **Litres and cost:** hidden until the car's consumption is set (step 4). The result card says so instead of using a guessed default.
- **New route:** added inline on the Start trip screen (name field + Add), not in a dialog. Route management (rename, delete, assign a route to an old trip) comes with step 3.
- **Open trip:** stored in the database, so it survives the app being closed. Only one trip can be open; it can be ended or discarded from the home screen.
- **Database:** schema v1 is exported to `app/schemas/`. From now on, schema changes need real Room migrations so installed data is kept.
- **Signing:** debug builds use the committed `app/debug.keystore` (the standard Android debug key), so new builds install over old ones without losing data.

## Decisions made while building step 3 (routes)
- **Navigation:** bottom tabs, **Trips** (the home screen) and **Routes**. Other screens open on top without the tabs.
- **Routes list:** each route with its counted trips, its usual (median) ratio and the last trip's verdict. Routes are still created from Start trip only.
- **Route page:** usual ratio and trip count, the trend (only with more than 5 trips), a ratio chart, stats, a "what makes a difference" breakdown, and the route's trips.
- **Chart:** the x axis is trip order (not dates), so bursts of trips don't squash the chart. It needs 2 comparable trips. The shaded band is the ±15% "normal" range and the dashed line is the usual ratio.
- **Breakdown:** for each tag, the median ratio of trips with the tag vs without it. Also by time of day, using the trip's start time in the phone's time zone: morning 5–10, midday 10–16, evening 16–21, night 21–5. A group is shown only with at least 2 trips (for tags, on both sides).
- **Rename:** inline on the route page. Names are unique ignoring case; changing only the case of a route's own name is allowed.
- **Delete route:** the route's trips (an open one included) are kept, as "No route".
- **Change a trip's route:** from the trip result ("Change route"), to any route or to No route. Verdicts recompute against the new route's history.

## Decisions made while building step 4 (settings and costs)
- **Settings:** opened with the ⚙ icon on the Trips and Routes tabs. Car: average consumption (L/100 km), the range rounding step (now changeable at any time) and the currency symbol ("€" by default). Saving recalculates every trip.
- **Empty consumption** means "not set": litres and costs hide, and trip results link to Settings instead.
- **Fuel price:** entered manually, valid from a date (today by default, back-dating allowed, no future dates). One price per date; adding another for the same date replaces it. Prices can be deleted.
- **Costing rule:** each trip uses the price valid on the day it started. Trips before the first price use the earliest price.
- **Where costs show:** trip result (fuel L, cost, cost of extra range), trip rows, the routes list (average cost) and the route page (average, total and cost of extra range).
- **No database change** was needed for this step; the existing tables already held these values.

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
  - `Route(id, name)` (unique name, ignoring case)
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
- **Automatic fuel price (Italy):** MIMIT "Osservaprezzi carburanti" open data (IODL 2.0). It publishes daily, at about 08:00, every station's prices (`prezzo_alle_8.csv`) and a station registry (`anagrafica_impianti_attivi.csv`). The separator changed on 10 Feb 2026, so detect it from the header.
  - Idea: the median of self-service Benzina prices in the user's province, counting only reports from the last 8 days and dropping typos outside 0.80–4.00 €/L. Fall back to the national median below 5 stations.
  - Fetch once a day on app open, Wi-Fi only if the files aren't compressed. Cache the province's station IDs and refresh the registry weekly.
  - A manual price for a date always beats the automatic one.
  - Not built yet: the format needs checking against the real files first.
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
