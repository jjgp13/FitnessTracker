# 🏋️ HealthSync AI

A personalized Android health & fitness tracker that syncs morning physiological metrics from Health Connect and Eight Sleep, then uses Google Gemini AI to generate tailored daily workout and nutrition plans. Built with Jetpack Compose and Material 3.

## ✨ Features

- **Health Data Aggregation** — Syncs sleep, HRV, heart rate, steps, weight, body fat, and blood pressure from Health Connect and Eight Sleep
- **AI-Powered Daily Plans** — Gemini 1.5 Pro analyzes your morning metrics to generate personalized training and nutrition recommendations
- **Recovery Detection** — Flags poor recovery and pivots to Active Recovery if HRV drops >10% below your 7-day average
- **Personalized Nutrition** — 3 meals + 2 snacks with macro targets (40C/30P/30F), tailored to dietary preferences
- **Morning Briefing** — A single dashboard displaying raw metrics and AI-generated recommendations
- **Background Sync** — WorkManager fetches data on wake-up for automatic plan generation

## 📱 Screenshots

*Coming soon*

## 🏗️ Architecture

The app follows **Clean Architecture** with three layers:

```
app/src/main/java/com/healthsync/ai/
├── data/                  # Data sources & repository implementations
│   ├── healthconnect/     # Health Connect API integration
│   ├── local/             # Room database (DAOs, entities)
│   ├── remote/
│   │   ├── eightsleep/    # Eight Sleep API (sleep stages & HRV)
│   │   └── gemini/        # Gemini AI client & prompt builder
│   └── repository/
├── domain/                # Business logic
│   ├── model/             # DailyPlan, Workout, NutritionPlan, UserProfile
│   ├── repository/        # Repository interfaces
│   └── usecase/           # FetchMorningMetrics, GenerateDailyPlan, etc.
├── di/                    # Hilt dependency injection modules
├── ui/
│   ├── components/        # Reusable composables (MetricCard, WorkoutCard)
│   ├── navigation/        # NavGraph, Screen routes
│   ├── screen/            # Auth, MorningBriefing, Workout, Nutrition, etc.
│   └── theme/             # Material 3 theming
└── worker/                # WorkManager background tasks
```

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| **UI** | Jetpack Compose, Material 3, Compose Navigation |
| **AI** | Firebase AI SDK (Gemini 1.5 Pro) |
| **Auth** | Firebase Auth, Google Sign-In |
| **Database** | Room |
| **Networking** | Retrofit, OkHttp, Kotlinx Serialization |
| **DI** | Hilt |
| **Health** | Health Connect API |
| **Background** | WorkManager |
| **Security** | EncryptedSharedPreferences |
| **Testing** | JUnit 5, MockK, Turbine |

## 📋 Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Min SDK 28 / Target SDK 35

## 🚀 Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/jjgp13/FitnessTracker.git
   cd FitnessTracker
   ```

2. **Set up Firebase**
   - Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
   - Enable Authentication (Google Sign-In) and Firestore
   - Enable the Gemini API via Firebase AI
   - Download `google-services.json` and place it in the `app/` directory

3. **Configure Eight Sleep (optional)**
   - Add your Eight Sleep credentials in the app's Settings screen after signing in

4. **Build and run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or open the project in Android Studio and run on a device/emulator with API 28+.

## 🔑 Configuration

| File | Purpose |
|------|---------|
| `google-services.json` | Firebase configuration (gitignored) |
| `local.properties` | Android SDK path (gitignored) |

## 📄 License

This project is for personal use.
