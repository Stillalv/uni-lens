# UniLens 🔍

**Real-Time Floating Phone & Email OCR Scanner for Android**

UniLens is an offline-first Android Native application engineered to detect international phone numbers and email addresses from camera feeds in real time, similar to a QR scanner. It includes a floating widget bubble that enables scanning directly from any application (Chrome, WhatsApp, Telegram, etc.) without leaving your workflow.

---

## ✨ Features

- ⚡ **Real-Time On-Device OCR**: Powered by Google ML Kit Latin Bundled Model. 100% offline, zero server reliance.
- 📱 **Strict Phone Detection**:
  - **Mandatory `+` prefix**: strictly rejects numbers without `+` (e.g. `08123456789`, `628123456789`).
  - **Separator normalization**: Normalizes formatted numbers (`+62 812-3456-7890` &rarr; `+6281234567890`).
  - **False positive rejection**: Rejects floating numbers, timestamps, prices, and years (`24.28`, `11.1269`, `0.028$`, `2026`).
- ✉️ **Strict Email Detection**:
  - Validates `name@domain.extension` standard (rejects `admin@`, `@gmail.com`, `admin@gmail`).
  - Bounded candidate cleaning with trailing sentence punctuation stripping.
- 🖼️ **Attach Image & Gallery OCR**:
  - Pick any image from Gallery or file picker to scan and extract phone and email without turning on the camera.
  - Zero storage permission required (uses Android Photo Picker / SAF).
- 🫧 **Floating Widget Bubble**:
  - Movable bubble overlay (`TYPE_APPLICATION_OVERLAY` for API 26+, `TYPE_PHONE` fallback for API 23-25).
  - **Decoupled Architecture**: Camera is 100% idle while the bubble sits on screen. Zero camera leaks or unnecessary background battery drain.
- 🎯 **Visual Bounding Boxes & Stability**:
  - Real-time highlight boxes on detected text items.
  - Temporal debounce prevents visual flickering.
  - Deduplicated by unique entity keys.
- 📋 **One-Tap Copy**:
  - Quick `[COPY]` buttons for detected entities.
  - Optional subtle haptic feedback when a new entity is recognized.
- 🚀 **GitHub Release Auto-Updater**:
  - Checks for newer versions via GitHub Releases API.
  - In-app download with real-time progress bar.
  - **SHA-256 Checksum Verification** before launching Android Package Installer.

---

## 🏗️ Architecture

```
UniLens (com.unilens.app)
├── Presentation Layer (Jetpack Compose + Material 3)
│   ├── MainActivity & ScannerActivity
│   ├── HomeScreen, ScannerScreen, SettingsScreen
│   ├── BoundingBoxOverlay & UpdateDialog
│   └── ViewModels (ScannerViewModel, SettingsViewModel)
├── Domain Layer (Business Logic & OCR Extraction)
│   ├── TextNormalizer (E.164 phone normalization, email cleaning)
│   ├── PhoneDetector (Strict '+' requirement, separator stripping, float/time rejection)
│   ├── EmailDetector (name@domain.ext pattern)
│   ├── DetectionEngine (Coordinates ML Kit blocks, merges, deduplicates)
│   └── TemporalStabilizer (Frame debounce, eliminates flicker)
├── Platform & Hardware Layer
│   ├── CameraX (ImageAnalysis, STRATEGY_KEEP_ONLY_LATEST, rate throttling)
│   ├── ML Kit Text Recognition (Bundled Latin, 100% on-device offline)
│   ├── FloatingBubbleService (WindowManager overlay, API 23-25 vs API 26+ compatibility)
│   └── UpdateManager (GitHub API latest release, SHA-256 verification, PackageInstaller)
└── CI/CD Layer (.github/workflows)
    ├── ci.yml (test + lint on push/PR)
    └── release.yml (assembleRelease + sign + SHA-256 + GitHub Release on tag v*)
```

---

## 📱 Permissions

UniLens follows the principle of minimal permissions:
- `android.permission.CAMERA`: Required for real-time video text recognition.
- `android.permission.SYSTEM_ALERT_WINDOW`: Required for the floating bubble overlay.
- `android.permission.FOREGROUND_SERVICE`: Required for running the floating bubble service.
- `android.permission.FOREGROUND_SERVICE_CAMERA`: Declared for Android 14+ compliance.
- `android.permission.VIBRATE`: For optional short haptic pulse on detection.
- `android.permission.INTERNET`: Only used to check GitHub Releases and download updates.
- `android.permission.REQUEST_INSTALL_PACKAGES`: To launch the system package installer for updates.

> **Privacy Notice**: UniLens processes all camera frames locally on your device. Camera images, video frames, and scanned text are never sent to any server or external service.

---

## 🛠️ CI/CD & Build via GitHub Actions

All compilation, testing, and packaging are handled automatically in the cloud by GitHub Actions without requiring local SDK resources:

1. **Continuous Integration (`ci.yml`)**:
   - Triggers on every `push` and `pull_request` to `main` / `master`.
   - Executes all unit tests (`./gradlew test`).
   - Builds debug APK and uploads build artifact.

2. **Automated Release (`release.yml`)**:
   - Triggers on Git tags formatted as `v*` (e.g. `v1.0.0`).
   - Runs test suite.
   - Builds release APK (`./gradlew assembleRelease`).
   - Signs APK (supports GitHub Secrets `KEYSTORE_BASE64` or automated release signing).
   - Generates SHA-256 checksum (`UniLens-vX.Y.Z.apk.sha256`).
   - Creates GitHub Release and attaches APK and checksum files.

### Triggering a Release
```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## 🧪 Unit Tests

- `PhoneDetectorTest`: Validates `+` requirement, separator handling, invalid number rejection, and false-positive avoidance (floats, timestamps).
- `EmailDetectorTest`: Validates RFC-compliant email structure, domain extension requirements, and invalid token rejection.
- `TextNormalizerTest`: Tests phone normalization to E.164 and safe whitespace trimming.
- `DetectionEngineIntegrationTest`: Validates the end-to-end multi-line text recognition scenario from PRD Section 49.

---

## 📄 License
MIT License.
