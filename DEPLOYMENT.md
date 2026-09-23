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

ClubLedger includes 39 JUnit 4 and Robolectric unit tests (run against an in-memory Room database) covering business logic, proportional allocation calculations, running-balance ledger consistency, invoice recalculation, CSV generators, and the permission engine.

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
   - **Package name:** `com.aistudio.clubledger.sports` *(must match `applicationId` in `app/build.gradle.kts`)*
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

## 🛡️ Firestore Security Rules

`firestore.rules` defines baseline security for the `clubs/{clubId}` tree used by `FirestoreSyncManager`: authenticated read, type-validated creates/updates, and blocked deletes on immutable collections (club metadata, users, teams, budgets, ledger). Deletes remain allowed on attendances and payments for corrections.

Deploy the rules to your Firebase project:

```bash
firebase login
./scripts/deploy-firestore-rules.sh
```

> ⚠️ Cloud sync assumes a signed-in user (Google/Apple/Facebook). Unauthenticated pushes are denied by design. Tighten the baseline (role-based writes, per-user ownership) before real multi-tenant use.

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

## 🏪 Google Play Console Release Checklist

1. **Create Application** in [Google Play Console](https://play.google.com/console):
   - Application Name: **ClubLedger** · Default Language: English (US) · App · Free.
2. **Store Listing Assets**:
   - App Icon: 512 × 512 px 32-bit PNG.
   - Feature Graphic: 1024 × 500 px JPG/PNG.
   - Phone Screenshots: minimum 2 (1080 × 1920 px or 1080 × 2400 px).
   - Short Description (≤ 80 chars): *Club treasury: attendance-based fair-share dues, payments & audit trail.*
   - Full Description: adapt the feature sections from [`README.md`](README.md).
3. **Data Safety Declaration**:
   - **Personal Info** (Name, Email): collected for club member identity; tied to Google/Apple/Facebook sign-in.
   - **Financial Info** (payment references/memos): user-provided payment records — declare purchase history / other financial info as applicable.
   - Data encrypted in transit (TLS/HTTPS via Firebase).
4. **Upload App Bundle** (`app-release.aab` from §4) → Release ➔ Production or Internal Testing → Add release notes → Roll out.

---

## 🔧 Troubleshooting

| Symptom | Probable Cause | Fix / Resolution |
| :--- | :--- | :--- |
| `java: command not found` / `No Java runtime` | Homebrew openjdk@17 is keg-only (not on PATH) | `export JAVA_HOME=/opt/homebrew/opt/openjdk@17` and `export PATH="$JAVA_HOME/bin:$PATH"` (see [LOCAL_SETUP_MAC.md](LOCAL_SETUP_MAC.md)) |
| `SDK location not found` | `ANDROID_HOME` unset and no `local.properties` | `export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools` (Homebrew SDK) or create `local.properties` with `sdk.dir=/path/to/sdk` |
| Keystore file `debug.keystore` not found | Custom debug signing config expects it at repo root (gitignored) | `keytool -genkeypair -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"` |
| `adb: command not found` | Platform-tools not on PATH | `export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"` (or the Homebrew SDK equivalent) |
| `gradlew: Permission denied` | Scripts lost their executable bit | `chmod +x gradlew scripts/*.sh` |
| First test run is slow / downloads a large jar | Robolectric fetching the API-34 `android-all` artifact | Expected on first run (~100 MB); cached afterwards |

---

## 📋 Quick Reference Commands

| Task | Command |
| :--- | :--- |
| **Full macOS Setup** | `./scripts/setup_mac.sh` |
| **Check Dev Environment** | `./scripts/dev_setup.sh` |
| **Run All Unit Tests** | `./scripts/run_tests.sh` |
| **Build Debug APK** | `./scripts/build_apk.sh` |
| **Install & Launch on Device** | `./scripts/run_app.sh` |
| **Clean & Rebuild** | `./scripts/clean_build.sh` |
| **Generate Release Keystore** | `./scripts/generate_release_keystore.sh` |
| **Build Release Bundle (AAB)** | `./scripts/build_release.sh aab` |
| **Build Release APK** | `./scripts/build_release.sh apk` |
| **Deploy Firestore Rules** | `./scripts/deploy-firestore-rules.sh` |
| **Clean Build Cache** | `./gradlew clean` |
