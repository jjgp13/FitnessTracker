# HealthSync AI - Architecture & Design

## Overview
Android app that aggregates health data from Health Connect, Eight Sleep (direct API), and Google Calendar, then uses Gemini 2.5 Pro (Firebase AI Logic) to generate schedule-aware, personalized daily training and nutrition plans.

## Architecture Pattern
**MVVM + Clean Architecture (3-layer)**

```
┌─────────────────────────────────────────────────┐
│                  UI Layer                        │
│  Jetpack Compose Screens + ViewModels            │
├─────────────────────────────────────────────────┤
│                Domain Layer                      │
│  Use Cases / Business Logic / Models             │
├─────────────────────────────────────────────────┤
│                Data Layer                        │
│  Repositories → Data Sources (Room, APIs, HC)    │
└─────────────────────────────────────────────────┘
```

## Tech Stack
| Concern                   | Technology                                |
|---------------------------|-------------------------------------------|
| Platform                  | Android (Min SDK 28+, Target 35)          |
| Language                  | Kotlin                                    |
| UI                        | Jetpack Compose + Material 3              |
| Navigation                | Compose Navigation                        |
| DI                        | Hilt                                      |
| Local DB                  | Room                                      |
| Cloud Backend             | Firebase (Auth + Firestore)               |
| Health Data               | Health Connect API                        |
| Sleep/HRV (Eight Sleep)   | Direct Eight Sleep API (unofficial REST)  |
| Calendar/Schedule         | Android CalendarContract API              |
| AI Engine                 | Firebase AI Logic SDK (Gemini 2.5 Pro)    |
| Background Work           | WorkManager                               |
| Networking                | Retrofit + OkHttp                         |
| Serialization             | Kotlinx Serialization                     |
| Security                  | EncryptedSharedPreferences (AndroidX)     |
| Testing                   | JUnit 5, MockK, Turbine, Compose Testing  |

## Package Structure
```
com.healthsync.ai/
├── di/                    # Hilt DI Modules
├── data/
│   ├── local/db/          # Room (entities, DAOs, database)
│   ├── local/mapper/      # Entity <-> Domain mappers
│   ├── remote/eightsleep/ # Eight Sleep API (auth, client, DTOs)
│   ├── remote/gemini/     # Gemini AI client + prompt builder
│   ├── remote/firebase/   # Firebase Auth/Firestore data sources
│   ├── healthconnect/     # Health Connect data source
│   ├── calendar/          # CalendarContract data source
│   └── repository/        # Repository implementations
├── domain/
│   ├── model/             # Domain models (HealthMetrics, DailyPlan, etc.)
│   ├── repository/        # Repository interfaces
│   └── usecase/           # Business logic use cases
├── ui/
│   ├── navigation/        # NavGraph, Screen routes
│   ├── theme/             # Material 3 theme (Color, Type, Theme)
│   ├── components/        # Reusable composables
│   ├── screen/            # Feature screens + ViewModels
│   └── state/             # Shared UI state classes
└── worker/                # WorkManager background jobs
```

## Key Design Decisions
1. **Room as source of truth** — offline-first, sync to Firestore when connected
2. **Eight Sleep direct API** — no third-party (Terra), uses unofficial REST endpoints with OAuth2
3. **Firebase AI Logic SDK** — not standalone Gemini SDK; managed through `google-services.json`
4. **CalendarContract** — reads device calendars (no extra OAuth), auto-detects sport events via keywords
5. **Schedule-aware AI** — Gemini receives full week schedule to plan around games/rest days
6. **EncryptedSharedPreferences** — secure storage for Eight Sleep credentials

## Firebase Configuration
- **Project**: healthsync-ai-5d3e6
- **Package**: com.healthsync.ai
- **Auth**: Google Sign-In (SHA-1 registered)
- **Gemini**: Enabled via Build → AI Logic
- **google-services.json**: Located at `app/google-services.json` (gitignored)
