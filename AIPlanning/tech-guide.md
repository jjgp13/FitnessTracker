# HealthSync AI - Technology Deep Dive

A guide for software engineers new to Kotlin/Android/Firebase. Explains how each technology works and how they interact in this project.

---

## 1. Kotlin (Language)

Kotlin is a statically-typed language on the JVM (like Java, but more concise). Key features we use:

### Data Classes
```kotlin
// Auto-generates equals(), hashCode(), toString(), copy()
data class HealthMetrics(
    val date: LocalDate,
    val hrvMs: Double,
    val steps: Int
)
```
Equivalent to a Java class with ~50 lines of boilerplate.

### Null Safety
```kotlin
val name: String = "John"     // Cannot be null
val nickname: String? = null  // Nullable — must handle with ?. or !!
nickname?.length              // Safe call — returns null if nickname is null
nickname ?: "Unknown"         // Elvis operator — default if null
```

### Coroutines (Async/Await)
Kotlin's answer to async programming. No callbacks, no Rx. Looks like synchronous code.

```kotlin
// `suspend` means this function can pause and resume (like async in C#/JS)
suspend fun fetchData(): HealthMetrics {
    val sleep = healthConnect.readSleep()    // pauses here, resumes when done
    val hrv = eightSleep.getHrv()           // pauses here too
    return HealthMetrics(sleep, hrv)         // returns when both complete
}

// Launch a coroutine from a ViewModel
viewModelScope.launch {
    val metrics = fetchData()  // runs on background thread automatically
    _uiState.value = metrics   // updates UI on main thread
}
```

### Flow (Reactive Streams)
Like RxJava Observables or JS Streams, but simpler:

```kotlin
// A Flow emits values over time
fun getMetrics(): Flow<List<HealthMetrics>> = dao.getAll()  // Room returns Flows

// Collect (subscribe) in a composable
val metrics by viewModel.metrics.collectAsStateWithLifecycle()
```

---

## 2. Jetpack Compose (UI Framework)

Declarative UI — you describe WHAT the UI looks like, not HOW to build it. Like React/SwiftUI.

### No XML Layouts
Everything is Kotlin functions annotated with `@Composable`:

```kotlin
@Composable
fun MetricCard(label: String, value: String) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

// Usage — just call the function
MetricCard(label = "HRV", value = "45ms")
```

### State Management
UI recomposes (re-renders) when state changes:

```kotlin
@Composable
fun Counter() {
    var count by remember { mutableStateOf(0) }  // local state
    Button(onClick = { count++ }) {
        Text("Clicked $count times")
    }
}
```

### How it connects to ViewModels
```kotlin
// ViewModel exposes StateFlow
class MyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()
}

// Composable collects it
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Text(state.title)  // automatically re-renders when state changes
}
```

---

## 3. Hilt (Dependency Injection)

Hilt is Google's DI framework for Android. It automatically creates and provides dependencies.

### The Problem It Solves
Without DI, you'd manually create everything:
```kotlin
// BAD — manual creation, tightly coupled
val database = Room.databaseBuilder(...).build()
val dao = database.metricsDao()
val apiService = Retrofit.Builder().build().create(ApiService::class.java)
val repository = HealthMetricsRepositoryImpl(dao, apiService)
val viewModel = MorningBriefingViewModel(repository)
```

### How Hilt Works
1. **Modules** define HOW to create things:
```kotlin
@Module
@InstallIn(SingletonComponent::class)  // lives for entire app lifetime
object DatabaseModule {
    @Provides
    @Singleton  // only one instance ever
    fun provideDatabase(@ApplicationContext context: Context): HealthSyncDatabase {
        return Room.databaseBuilder(context, HealthSyncDatabase::class.java, "db").build()
    }
}
```

2. **@Inject** tells Hilt WHAT a class needs:
```kotlin
class HealthMetricsRepositoryImpl @Inject constructor(
    private val dao: HealthMetricsDao,           // Hilt finds this from DatabaseModule
    private val eightSleep: EightSleepDataSource // Hilt finds this from NetworkModule
) : HealthMetricsRepository
```

3. **@HiltViewModel** auto-injects ViewModels:
```kotlin
@HiltViewModel
class MorningBriefingViewModel @Inject constructor(
    private val fetchMetrics: FetchMorningMetricsUseCase
) : ViewModel()
```

4. Hilt wires everything automatically at compile time via KSP (Kotlin Symbol Processing).

### Our DI Modules
```
AppModule          → EncryptedSharedPreferences
DatabaseModule     → Room database + DAOs
NetworkModule      → Retrofit clients for Eight Sleep API
FirebaseModule     → FirebaseAuth, Firestore
CalendarModule     → ContentResolver, SharedPreferences
AiModule           → Gemini GenerativeModel
RepositoryModule   → Binds interfaces to implementations
```

---

## 4. Room (Local Database)

Room is an ORM (like SQLAlchemy, Prisma, Entity Framework) for SQLite on Android.

### Three Layers

**Entity** = Table definition:
```kotlin
@Entity(tableName = "health_metrics")
data class HealthMetricsEntity(
    @PrimaryKey val date: String,
    val hrvMs: Double,
    val steps: Int
)
```

**DAO** = Queries (like a repository pattern):
```kotlin
@Dao
interface HealthMetricsDao {
    @Query("SELECT * FROM health_metrics WHERE date = :date")
    suspend fun getByDate(date: String): HealthMetricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metrics: HealthMetricsEntity)

    @Query("SELECT * FROM health_metrics ORDER BY date DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<HealthMetricsEntity>
}
```

**Database** = Glue:
```kotlin
@Database(entities = [HealthMetricsEntity::class], version = 1)
abstract class HealthSyncDatabase : RoomDatabase() {
    abstract fun healthMetricsDao(): HealthMetricsDao
}
```

### Why Room + Domain Models?
We have separate Entity (database) and Domain (business logic) models to keep layers independent. Mappers convert between them:
```
HealthMetricsEntity  ←→  HealthMetrics (domain)
       ↕                        ↕
   Room/SQLite              Use Cases / UI
```

---

## 5. Firebase (Cloud Backend)

### Firebase Auth
Handles Google Sign-In. We don't manage passwords or tokens — Firebase does it all:
```kotlin
val credential = GoogleAuthProvider.getCredential(idToken, null)
FirebaseAuth.getInstance().signInWithCredential(credential)
// Now FirebaseAuth.currentUser is set globally
```

### Cloud Firestore
NoSQL document database (like MongoDB). We sync user profiles:
```kotlin
// Write
firestore.collection("users").document(userId).set(profileMap)

// Read (real-time listener)
firestore.collection("users").document(userId)
    .addSnapshotListener { snapshot, _ ->
        val profile = snapshot?.toObject(UserProfile::class.java)
    }
```

### Firebase AI Logic (Gemini)
Provides Gemini 2.5 Pro access through the Firebase SDK — no separate API key management:
```kotlin
val model = Firebase.ai(backend = GenerativeBackend.googleAI())
    .generativeModel("gemini-2.5-pro")

val response = model.generateContent("Generate a workout plan")
val text = response.text  // AI response
```

### How google-services.json Works
This file contains your Firebase project config (project ID, API keys, OAuth client IDs). The Google Services Gradle plugin reads it at build time and injects the config into your app. You never manually handle API keys.

---

## 6. Retrofit + OkHttp (Networking)

Retrofit turns REST APIs into Kotlin interfaces. OkHttp handles the HTTP layer.

### Define the API as an Interface
```kotlin
interface EightSleepApiService {
    @GET("v1/users/{userId}/sleeps")
    suspend fun getSleepMetrics(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): SleepMetricsResponse
}
```

### Retrofit Creates the Implementation
```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://client-api.8slp.net/")
    .addConverterFactory(json.asConverterFactory(...))
    .build()

val api = retrofit.create(EightSleepApiService::class.java)

// Now just call it like a regular function
val response = api.getSleepMetrics("Bearer $token", userId)
```

### Kotlinx Serialization
Handles JSON ↔ Kotlin conversion (like Jackson/Gson):
```kotlin
@Serializable
data class SleepMetricsResponse(
    val sleeps: List<SleepSession> = emptyList()
)
// Retrofit automatically deserializes JSON → this data class
```

---

## 7. Health Connect API

Android's unified API for health data. Apps (Fitbit, Withings) write data → Health Connect → our app reads it.

```
Fitbit App → writes steps, HR, sleep → Health Connect
Withings App → writes weight, BP → Health Connect
Our App → reads all of it via HealthConnectClient
```

```kotlin
val client = HealthConnectClient.getOrCreate(context)

// Read steps for today
val response = client.readRecords(
    ReadRecordsRequest(
        recordType = StepsRecord::class,
        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
    )
)
val totalSteps = response.records.sumOf { it.count }
```

Permissions are declared in AndroidManifest.xml and requested at runtime (like camera permissions).

---

## 8. WorkManager (Background Processing)

Runs tasks even when the app is closed. Survives device reboots.

```kotlin
class MorningDataFetchWorker(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val metrics = repository.fetchTodayMetrics()
        return Result.success()
    }
}

// Schedule it — runs every morning
val request = PeriodicWorkRequestBuilder<MorningDataFetchWorker>(
    repeatInterval = 1, TimeUnit.DAYS
).build()
WorkManager.getInstance(context).enqueue(request)
```

---

## 9. How Everything Connects (Data Flow)

### Morning Briefing Flow
```
1. App opens → MorningBriefingViewModel created
2. ViewModel calls FetchMorningMetricsUseCase
3. UseCase calls HealthMetricsRepository.fetchTodayMetrics()
4. Repository reads from:
   ├── HealthConnectDataSource → Health Connect API → Fitbit/Withings data
   └── EightSleepDataSource → Eight Sleep REST API → HRV/sleep data
5. Repository merges data → saves to Room → returns HealthMetrics
6. ViewModel calls DetermineRecoveryStatusUseCase(metrics) → RecoveryStatus
7. ViewModel calls GetWeekScheduleUseCase → CalendarRepository → CalendarContract
8. ViewModel calls GenerateDailyPlanUseCase(metrics, recovery, schedule)
9. UseCase calls GeminiClient with PromptBuilder output
10. Gemini returns JSON → parsed to DailyPlan → cached in Room
11. ViewModel updates UI state → Compose recomposes → user sees their plan
```

### Architecture Layers in Action
```
┌──────────────────────────────────────────────────────────────┐
│  UI: MorningBriefingScreen (Compose)                         │
│  ← observes StateFlow from ViewModel                         │
├──────────────────────────────────────────────────────────────┤
│  ViewModel: MorningBriefingViewModel                         │
│  ← calls Use Cases, manages UI state                         │
├──────────────────────────────────────────────────────────────┤
│  Use Cases: FetchMorningMetrics, DetermineRecovery,          │
│             GetWeekSchedule, GenerateDailyPlan               │
│  ← orchestrate business logic                                │
├──────────────────────────────────────────────────────────────┤
│  Repositories: HealthMetrics, Calendar, DailyPlan            │
│  ← merge multiple data sources, cache in Room                │
├──────────────────────────────────────────────────────────────┤
│  Data Sources:                                               │
│  ├── HealthConnectDataSource → Health Connect API            │
│  ├── EightSleepDataSource → Eight Sleep REST API (Retrofit)  │
│  ├── CalendarDataSource → Android CalendarContract           │
│  ├── GeminiClient → Firebase AI Logic SDK                    │
│  └── Room Database → SQLite (local cache)                    │
└──────────────────────────────────────────────────────────────┘
```

### Dependency Injection Wiring
```
Hilt creates everything at app startup:

AppModule ──────────→ EncryptedSharedPreferences
DatabaseModule ─────→ Room DB → HealthMetricsDao, DailyPlanDao, UserProfileDao
NetworkModule ──────→ OkHttp → Retrofit → EightSleepAuthService, EightSleepApiService
FirebaseModule ─────→ FirebaseAuth, FirebaseFirestore
CalendarModule ─────→ ContentResolver, SharedPreferences
AiModule ───────────→ Gemini GenerativeModel

RepositoryModule binds:
  AuthRepository           ← AuthRepositoryImpl(FirebaseAuth)
  UserProfileRepository    ← UserProfileRepositoryImpl(UserProfileDao, Firestore)
  HealthMetricsRepository  ← HealthMetricsRepositoryImpl(HC, EightSleep, DAO)
  CalendarRepository       ← CalendarRepositoryImpl(CalendarDS, SportDetector, Prefs)
  DailyPlanRepository      ← DailyPlanRepositoryImpl(GeminiClient, DAO, Firestore)
```
