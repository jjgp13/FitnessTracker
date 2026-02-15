# Project: HealthSync AI - Personalized Performance Engine

## 1. Objective
Build an Android application that aggregates health data from Health Connect (Fitbit, Withings, Eight Sleep) and utilizes the Gemini 1.5 Pro API to generate hyper-personalized training and nutrition plans based on morning physiological metrics.

## 2. Technical Stack
* **Platform:** Android (Min SDK 28+)
* **Language:** Kotlin / Jetpack Compose
* **Data Hub:** Android Health Connect API
* **AI Engine:** Google AI SDK for Android (Gemini 1.5 Pro)
* **Work Manager:** For background data fetching and processing upon wake-up.

## 3. Data Integration Strategy
* **Fitbit/Withings:** Sync via Health Connect (Steps, HR, Weight, Body Fat, Blood Pressure).
* **Eight Sleep:** Integration via a custom API wrapper or Terra API to pull Sleep Stages and HRV.
* **Contextual Input:** Current weather in Seattle (to determine indoor vs. outdoor soccer/volleyball drills).

## 4. User Profile & Fitness Constraints
* **Demographics:** 32-year-old Male.
* **Primary Sports:** Soccer (Midfielder/Forward logic), Competitive Co-ed Volleyball.
* **Strength Focus:** Functional strength (Trap bar deadlifts, Nordic curls, Air squats).
* **Dietary Preferences:** High-protein Mexican and American cuisine (e.g., Pozole, Chipotle-style bowls, lean burgers).

## 5. Gemini System Instruction (The "Brain")
The model must act as a "High-Performance Sports Scientist and Nutritionist." 

### AI Instruction Set:
1. **Analyze:** Look at Sleep Quality (Deep/REM), HRV, and Blood Pressure. If HRV is >10% below the 7-day rolling average, pivot the training to "Active Recovery."
2. **Train:** Generate a workout for a 32-year-old focusing on explosive power for soccer and verticality for volleyball. 
3. **Fuel:** Recommend 3 meals and 2 snacks. Incorporate Mexican/American flavors while maintaining a specific macro split (e.g., 40% Carb, 30% Protein, 30% Fat).
4. **Format:** Always return a valid JSON object matching the `DailyPlan` schema.

## 6. Development Milestones (Copilot CLI Instructions)
1. Scaffold a Jetpack Compose project with Navigation and Hilt for DI.
2. Implement Health Connect permission handling and data repository.
3. Create the Gemini API client using the Vertex AI / Google AI SDK.
4. Build a "Morning Briefing" UI that displays the raw metrics vs. the AI recommendation.