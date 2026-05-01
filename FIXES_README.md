# SPPMS (Parentral) - Bug Fixes Applied

## Bugs Fixed

### 1. Registration Error / "Email Already Exists"
**Problem:** Firebase Auth was showing cryptic errors. If someone tried to register the same email again, it showed a confusing message.

**Fix in `RegisterActivity.kt`:**
- Added specific error handling for `FirebaseAuthUserCollisionException` → shows clear message: "This email is already registered. Please login instead."
- Added loading state (button disabled + text changes to "Registering...") to prevent double-taps
- If Firestore save fails after Auth creation, the Auth account is automatically deleted (keeps data clean)
- Better error messages for weak password, invalid email format

### 2. Login Failed
**Problem:** Login was failing with generic "Login failed" message. No distinction between wrong password vs email not found.

**Fix in `LoginActivity.kt`:**
- Added `FirebaseAuthInvalidUserException` → "No account found with this email. Please register first."
- Added `FirebaseAuthInvalidCredentialsException` → "Wrong password. Please try again."
- Auto-fills email field when coming from registration screen
- Checks if user is already logged in on launch → goes directly to dashboard
- Added loading state (button disabled during login)
- If Firestore user profile not found, signs out gracefully

### 3. Location Not Tracking
**Problem:** `ChildDashboardActivity` used `lastLocation` which can return `null` (especially on emulator or when GPS is cold). `LocationActivity` had **hardcoded** child name and coordinates — not reading from Firebase at all!

**Fix in `ChildDashboardActivity.kt`:**
- Replaced `lastLocation` (one-shot, often null) with `requestLocationUpdates()` — now gets continuous GPS updates every 30 seconds
- `LocationCallback` saves to Firestore whenever a new location arrives
- Also requests last location immediately as a fallback
- Added `ACCESS_BACKGROUND_LOCATION` permission to AndroidManifest

**Fix in `LocationActivity.kt`:**
- **Complete rewrite** — removed all hardcoded data
- Now queries Firestore for ALL children linked to the parent's email
- Shows live location coordinates, time since last update
- Provides "View on Map" button (opens `ChildLocationMapActivity`) and "Open in Google Maps" button
- Shows helpful message if child hasn't shared location yet

### 4. YouTube Graph Not Visible
**Problem:** `Last24HourHistoryActivity` was reading from a `usage_logs` Firestore collection that doesn't exist — the app saves data to the `users` collection directly.

**Fix in `Last24HourHistoryActivity.kt`:**
- Now reads YouTube usage from the `users` collection (where data is actually saved)
- Also checks `usage_logs` as a secondary source for historical data
- Chart now shows proper AM/PM hour labels
- Shows "No YouTube data yet" message instead of blank chart when no data exists
- Fixed bar colors to match purple theme (#7C4DFF)
- Added `animateY(1200)` animation

### 5. Reels Activity Query Bug
**Problem:** `ReelsActivity` was querying `db.collection("users").whereEqualTo("parentEmail", parentEmail)` — this correctly finds children, but was showing data from ALL users in some edge cases.

**Fix in `ReelsActivity.kt`:**
- Added `whereEqualTo("role", "child")` to ensure only children are returned
- Improved UI with colored status badges (orange "Watching Reels" / green "Stopped")
- Added alert banner when any child exceeds limit
- Better limit exceeded warning display

### 6. AndroidManifest — Accessibility Service Fix
**Problem:** `YouTubeMonitorService` extends `AccessibilityService` but the manifest declared it as a regular service without the accessibility intent-filter or meta-data.

**Fix in `AndroidManifest.xml`:**
- Added proper `BIND_ACCESSIBILITY_SERVICE` permission
- Added `android.accessibilityservice.AccessibilityService` intent-filter
- Added meta-data pointing to `@xml/accessibility_service_config`

**New file `res/xml/accessibility_service_config.xml`:**
- Configures the accessibility service to target YouTube
- Enables `canRetrieveWindowContent="true"` needed for reading video titles

---

## How to Set Up Firebase

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project `sppms2-9e2fb`
3. **Authentication:** Enable Email/Password sign-in method
4. **Firestore Rules** — Add these rules:
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null;
    }
    match /usage_logs/{logId} {
      allow read, write: if request.auth != null;
    }
  }
}
```
5. **Google Maps API Key:** Replace `YOUR_GOOGLE_MAPS_API_KEY` in `AndroidManifest.xml` with your actual Google Maps API key from [Google Cloud Console](https://console.cloud.google.com)

---

## How to Run

1. Open project in Android Studio
2. Sync Gradle
3. Update `YOUR_GOOGLE_MAPS_API_KEY` in `AndroidManifest.xml`
4. Run on a real device (location tracking works much better on real devices than emulators)
5. First: Register a **Parent** account
6. Then: Register a **Child** account — enter the parent's email in "Parent Email" field
7. Login as Child → grants location/usage permissions → location will update automatically
8. Login as Parent → view child's location, YouTube activity, reels status

---

## Notes
- Location tracking requires the child to have the app open and grant location permission
- YouTube title detection requires Notification Access + Accessibility Service to be granted
- The graph shows YouTube usage data from the current session — it will populate as the child uses YouTube
