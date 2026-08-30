# 🚀 ClubLedger: Local Development & Production Deployment Guide

This guide provides step-by-step instructions for setting up your local development environment, testing, and creating production-ready releases for the Google Play Store or direct APK distribution.

---

## 💻 1. Local Development Setup

### System Prerequisites
- **Operating System:** macOS, Linux, or Windows 10/11 (with WSL2 or PowerShell).
- **Java Development Kit (JDK):** **OpenJDK 17** (LTS) or higher.
- **Android SDK & Build Tools:** API Level 34+ (Android 14).
- **IDE:** [Android Studio Iguana / Jellyfish or newer](https://developer.android.com/studio).

---

### Step-by-Step Local Setup

#### 1. Clone the Repository
```bash
git clone <repository-url>
cd clubledger
```

#### 2. Verify Environment via Script
Run the automated environment checking script:
```bash
chmod +x scripts/*.sh
./scripts/dev_setup.sh
```

#### 3. Open in Android Studio
1. Launch **Android Studio**.
2. Select **Open** and choose the root directory of this project.
3. Allow Gradle to sync dependencies (`libs.versions.toml`).
4. Select an Android Virtual Device (AVD) running **Android API 26+** (API 34 recommended).
5. Click the **Run** button (`Shift + F10`) to launch the app.

#### 4. Running via Command Line
To build and install the debug APK directly to a connected device or emulator via ADB:
```bash
# Build debug APK
./gradlew assembleDebug

# Install directly on connected emulator / device
./gradlew installDebug
```

---

## 🧪 2. Testing & Quality Assurance

ClubLedger includes JVM unit tests and Robolectric tests for business logic, proportional allocation calculations, and ledger ledger consistency.

### Run JVM / Unit Tests
```bash
./scripts/run_tests.sh
# or directly via Gradle:
./gradlew :app:testDebugUnitTest
```

---

## ☁️ 3. Firebase & Cloud Multi-User Configuration (Optional)

ClubLedger operates in **Offline-First Room SQLite Mode** by default. To enable live multi-user sync and social authentication across physical devices:

1. Create a Firebase project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an **Android Application** with:
   - **Package name:** `com.aistudio.clubdues.kxmpzq`
   - **SHA-1 Fingerprint:** (Obtained via `./gradlew signingReport`)
3. Under **Authentication ➔ Sign-in method**, enable:
   - **Google**: Enable and select support email.
   - **Apple**: Enable and configure Apple Team ID & Service ID.
   - **Facebook**: Enable and supply Facebook App ID & App Secret from Meta Developers.
4. Enable **Cloud Firestore** in production or test rules.
5. Download `google-services.json` and place it in the `app/` directory:
   ```
   app/google-services.json
   ```
6. Re-run `./gradlew assembleDebug` or build in Android Studio.

---

## 📦 4. Production Release & Deployment

### Step A: Configure Production Signing Key

1. **Generate a Release Keystore** (if you don't already have one):
   ```bash
   keytool -genkey -v -keystore release.keystore -alias clubledger \
     -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Configure Environment Secrets or `gradle.properties`**:
   Never commit signing passwords into git. Set the following in your local `~/.gradle/gradle.properties` or CI/CD secrets:
   ```properties
   RELEASE_KEYSTORE_PATH=/path/to/release.keystore
   RELEASE_KEYSTORE_PASSWORD=your_store_password
   RELEASE_KEY_ALIAS=clubledger
   RELEASE_KEY_PASSWORD=your_key_password
   ```

---

### Step B: Build Production Artifacts

#### Option 1: Android App Bundle (`.aab`) — Recommended for Google Play
```bash
./scripts/build_release.sh aab
# or
./gradlew :app:bundleRelease
```
*Output artifact:* `app/build/outputs/bundle/release/app-release.aab`

#### Option 2: Standalone Release APK (`.apk`) — Direct Sideloading
```bash
./scripts/build_release.sh apk
# or
./gradlew :app:assembleRelease
```
*Output artifact:* `app/build/outputs/apk/release/app-release.apk`

---

## 🤖 5. Automated CI/CD (GitHub Actions)

A ready-to-use GitHub Actions workflow is located at `.github/workflows/android_ci_cd.yml`.

### Workflow Capabilities:
- Automatically triggers on pushes and pull requests to `main`.
- Sets up JDK 17 with Gradle caching.
- Executes `./gradlew testDebugUnitTest`.
- Assembles Debug APKs and uploads them as build artifacts.
- Automatically builds production `.aab` bundles on merges to `main`.

---

## 📋 Quick Reference Commands

| Task | Command |
| :--- | :--- |
| **Check Dev Environment** | `./scripts/dev_setup.sh` |
| **Run All Unit Tests** | `./scripts/run_tests.sh` |
| **Build Debug APK** | `./gradlew assembleDebug` |
| **Build Release Bundle (AAB)** | `./scripts/build_release.sh aab` |
| **Build Release APK** | `./scripts/build_release.sh apk` |
| **Clean Build Cache** | `./gradlew clean` |
