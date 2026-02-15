# HealthSync AI - API Reference

## Eight Sleep API (Unofficial)

### Authentication
```
POST https://auth-api.8slp.net/v1/tokens
Content-Type: application/json

{
  "client_id": "0894c7f33bb94800a03f1f4df13a4f38",
  "client_secret": "f0954a3ed5763ba3d06834c73731a32f15f168f47d4f164751275def86db0c76",
  "email": "<user_email>",
  "password": "<user_password>"
}

Response:
{
  "access_token": "...",
  "expires_in": 3600,
  "userId": "...",
  "token_type": "Bearer"
}
```

### Sleep Metrics
```
GET https://client-api.8slp.net/v1/users/{userId}/sleeps
Authorization: Bearer <access_token>
Query params: startDate, endDate (optional)

Response includes:
- Sleep sessions with start/end times
- Sleep stages (deep, REM, light)
- Timeseries: HRV, heart rate, respiratory rate
- Sleep score
```

### User Info
```
GET https://client-api.8slp.net/v1/users/{userId}
Authorization: Bearer <access_token>
```

### Security
- Credentials stored via EncryptedSharedPreferences (AES256)
- Token auto-refresh with 5-minute buffer before expiry
- All Eight Sleep code isolated in `data/remote/eightsleep/`

### Risk
Unofficial API — endpoints may change. Community projects tracking changes:
- pyEight: https://github.com/lukas-clarke/pyEight
- eightctl: https://github.com/steipete/eightctl
- Home Assistant: https://github.com/lukas-clarke/eight_sleep

---

## Firebase AI Logic (Gemini 2.5 Pro)

### SDK Setup
```kotlin
// build.gradle.kts
implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
implementation("com.google.firebase:firebase-ai")

// Usage
val model = Firebase.ai(backend = GenerativeBackend.googleAI())
    .generativeModel("gemini-2.5-pro")

val response = model.generateContent("Your prompt here")
```

### Structured Output
Use `responseMimeType = "application/json"` with schema to get DailyPlan JSON.

### API Key
Managed by Firebase — no manual key needed. Uses `google-services.json`.

---

## Health Connect API

### Permissions (declared in AndroidManifest.xml)
- READ_HEART_RATE
- READ_STEPS
- READ_SLEEP
- READ_WEIGHT
- READ_BODY_FAT
- READ_BLOOD_PRESSURE
- READ_RESTING_HEART_RATE
- READ_HEART_RATE_VARIABILITY

### Data Sources
- Fitbit → Health Connect: Steps, HR, Sleep, Weight
- Withings → Health Connect: Weight, Body Fat, Blood Pressure

---

## Android CalendarContract API

### Permission
```xml
<uses-permission android:name="android.permission.READ_CALENDAR" />
```

### Reading Events
```kotlin
val projection = arrayOf(
    CalendarContract.Events._ID,
    CalendarContract.Events.TITLE,
    CalendarContract.Events.DTSTART,
    CalendarContract.Events.DTEND,
    CalendarContract.Events.ALL_DAY,
    CalendarContract.Events.CALENDAR_DISPLAY_NAME
)

context.contentResolver.query(
    CalendarContract.Events.CONTENT_URI,
    projection, selection, args, sortOrder
)
```

Reads all synced calendars (Google Calendar, Outlook, etc.) — no OAuth needed.
