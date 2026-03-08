# 🧥 AI Waistcoat Measurement — Body Scan & Smart Size Recommender

An Android app that uses **Google MediaPipe Pose Landmarker** to scan a user's body from a live camera image, calculate real-world body measurements in centimetres, recommend the correct waistcoat size and fit type, and present a curated product list — all in a fully reactive **Jetpack Compose** + **MVVM** architecture.

---

## 📖 Description

This app eliminates the guesswork of online clothing sizing. By pointing the camera at a person's full body, the AI pipeline detects 33 skeletal pose landmarks, derives shoulder width, chest circumference, waist, and torso length using a pixel-to-cm scale factor derived from the user's known height, then maps those measurements to standard waistcoat sizing (36–46) and recommends Slim Fit or Regular Fit. The result flows through a 5-screen shopping journey ending at a cart.

---

## 🗂️ Project Structure

```
app/
├── assets/
│   └── pose_landmarker_full.task        # MediaPipe model file (excluded from git — see .gitignore)
└── src/main/java/com/example/aiwaistcoatmeasurment/
    ├── MainActivity.kt                  # Entry point; initializes PoseLandmarker & navigation
    ├── DataModel.kt                     # Data classes: BodyMeasurements, SizeRecommendation, WaistcoatProduct
    ├── PoseLandmarkProcessor.kt         # Core AI pipeline: image → landmark detection → measurements in cm
    ├── SizeRecommendationEngine.kt      # Maps chest circumference + body shape → size & fit type
    ├── BodyScanViewModel.kt             # MVVM ViewModel: ScanState flow + product generation
    └── CameraScannerScreen.kt           # Camera UI composable (scanner screen)
```

---

## 🧠 Architecture & Data Flow

```
Camera Frame (Bitmap)
        │
        ▼
PoseLandmarkProcessor
  ├── MediaPipe detects 33 landmarks
  ├── Extracts: shoulder, hip, nose, ankle points
  ├── Calculates pixel distances (Euclidean)
  ├── Derives scale factor: userHeightCm / bodyHeightPx
  └── Returns BodyMeasurements (chest, waist, shoulder, torso, bodyShape, confidence)
        │
        ▼
SizeRecommendationEngine
  ├── Maps chestCm → waistcoat size (36–46)
  ├── Maps bodyShape → fit type (Slim / Regular)
  └── Maps visibility confidence → "High" / "Medium"
        │
        ▼
BodyScanViewModel (StateFlow)
  ├── ScanState: Idle → Capturing → Processing → Success | Error
  └── Generates WaistcoatProduct list based on recommendation
        │
        ▼
Navigation (5 screens)
  Scanner → Results → Products → Product Detail → Cart
```

---

## 🧩 Key Concepts Covered

| Concept | Implementation |
|---|---|
| On-device AI | MediaPipe `PoseLandmarker` in `IMAGE` mode |
| Pose Landmark Extraction | 33-point skeleton; landmarks 0, 11, 12, 23, 24, 27 used |
| Pixel → Real-world Scale | `scaleFactor = userHeightCm / bodyHeightPx` |
| Chest Estimation | `shoulderWidthPx × scaleFactor × 2.1` |
| Waist Estimation | `hipWidthPx × scaleFactor × 1.8` |
| Body Shape Classification | Chest/waist ratio: >1.15 Athletic, >1.05 Fit, else Regular |
| Size Chart Mapping | Standard UK/EU waistcoat sizes 36–46 by chest cm |
| MVVM + StateFlow | `BodyScanViewModel` with sealed `ScanState` class |
| Compose Navigation | 5-route `NavHost` with state passed via `remember` |
| Runtime Camera Permission | `accompanist-permissions` `rememberPermissionState` |
| Model Loading | Copied from `assets/` to `cacheDir` for MediaPipe path API |
| Resource Cleanup | `poseLandmarker.close()` in `onDestroy()` |
| Edge-to-Edge UI | `enableEdgeToEdge()` + `systemBarsPadding()` |

---

## 🖥️ Screen Flow

### 📷 Scanner Screen (`CameraScannerScreen.kt`)
Live camera feed. Captures a full-body image and passes the `Bitmap` to `BodyScanViewModel.processCapturedImage()`.

### 📊 Results Screen
Displays extracted measurements (chest, waist, shoulder, torso in cm), body shape, confidence level, and recommended size + fit type.

### 🛍️ Products Screen
Shows a dynamically generated list of 3 waistcoat products pre-filtered to the user's recommended size and fit type.

### 🔍 Product Detail Screen
Full product view showing name, price, fit type, and the user's measurements for context. Includes **Add to Cart** button.

### 🛒 Cart Screen
Confirms the selected product. Checkout hook is present but intentionally left for extension.

---

## ⚙️ Component Deep Dives

### `PoseLandmarkProcessor.kt` — The AI Core

```kotlin
// Scale factor derived from known height vs detected body height in pixels
val scaleFactor = userHeightCm / bodyHeightPx

// Estimation multipliers (empirically derived)
val chestCm  = shoulderWidthPx * scaleFactor * 2.1f
val waistCm  = hipWidthPx      * scaleFactor * 1.8f
```

Uses Euclidean distance between normalized landmark coordinates, denormalized by image dimensions.

### `SizeRecommendationEngine.kt` — Sizing Logic

```
chestCm < 90  → size 36
chestCm < 95  → size 38
chestCm < 100 → size 40
chestCm < 105 → size 42
chestCm < 110 → size 44
else          → size 46

bodyShape = "Athletic" | "Fit"  → "Slim Fit"
bodyShape = "Regular"           → "Regular Fit"
```

### `BodyScanViewModel.kt` — State Machine

```kotlin
sealed class ScanState {
    object Idle        : ScanState()
    object Capturing   : ScanState()
    object Processing  : ScanState()
    data class Success(...) : ScanState()
    data class Error(...)   : ScanState()
}
```

Runs `processor.processImage()` on a coroutine. On success, triggers product generation.

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- Minimum SDK: 26 (Android 8.0)
- Kotlin 1.9+
- Jetpack Compose BOM
- MediaPipe Tasks Vision SDK

### Dependencies (`build.gradle`)

```kotlin
implementation("com.google.mediapipe:tasks-vision:0.10.14")
implementation("com.google.accompanist:accompanist-permissions:0.34.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.navigation:navigation-compose:2.7.7")
```

### Required Manifest

```xml
<uses-permission android:name="android.permission.CAMERA" />

<application ...>
    <!-- Required for MediaPipe model inference -->
    <uses-library android:name="org.apache.http.legacy" android:required="false"/>
</application>
```

### Model Setup

Download the MediaPipe Pose Landmarker model and place it in `app/src/main/assets/`:

```bash
# Download from MediaPipe
wget https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/latest/pose_landmarker_full.task \
     -O app/src/main/assets/pose_landmarker_full.task
```

> The model file is excluded from version control via `.gitignore` due to its large size.

### Clone & Run

```bash
git clone https://github.com/your-username/ai-waistcoat-measurement-android.git
cd ai-waistcoat-measurement-android
# Add the model file to assets/ (see above)
# Open in Android Studio and run on a physical device (API 26+)
```

> ⚠️ Camera and pose detection work best on a **physical device**. Emulators may not support the camera or MediaPipe hardware acceleration.

---

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **AI / ML:** Google MediaPipe Tasks Vision — Pose Landmarker
- **Architecture:** MVVM + StateFlow
- **Navigation:** Jetpack Navigation Compose
- **Permissions:** Accompanist Permissions
- **Concurrency:** Kotlin Coroutines + `viewModelScope`

---

## 📚 What I Learned

- How to integrate **MediaPipe on-device AI** into an Android Compose app
- How to extract and **denormalize pose landmarks** from normalized coordinates
- How to derive **real-world measurements from pixel distances** using a height-based scale factor
- How to model a **multi-step async UI state machine** using sealed classes and `StateFlow`
- How to safely copy large **model assets from `assets/` to `cacheDir`** for MediaPipe's file-path API
- How to structure a **5-screen shopping flow** passing data between Composables without deep nav arguments

---

## ⚠️ Known Limitations & Future Improvements

- Default height is hardcoded to `172f` cm — a height input screen would improve accuracy
- Chest and waist use **estimation multipliers** (`×2.1`, `×1.8`) — calibration with real measurements would increase precision
- Products are **hardcoded** in `generateProducts()` — a real backend/API integration is the natural next step
- Checkout in `CartScreen` is a stub — payment integration (Stripe, etc.) can be added
- Detection requires a **full-body, well-lit, single-person** frame for best results

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).