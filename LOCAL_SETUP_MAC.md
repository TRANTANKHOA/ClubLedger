# 🍏 macOS Local Development Guide - ClubLedger

Step-by-step instructions to set up, build, test, and run **ClubLedger** locally on **macOS** (Apple Silicon and Intel).

---

## ⚡ Quick Start

```bash
chmod +x gradlew scripts/*.sh
./scripts/setup_mac.sh
```

`setup_mac.sh` installs and verifies everything on a clean Mac: Homebrew, OpenJDK 17, the Android SDK (Studio or Homebrew commandlinetools), `adb`, and script permissions. Already set up? `./scripts/dev_setup.sh` just verifies the environment.

---

## 🛠️ Prerequisites & Manual Setup

### 1. Install Homebrew (if missing)
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

### 2. Install JDK 17
```bash
brew install openjdk@17
```
Homebrew's openjdk@17 is **keg-only** — add it to your shell profile (`~/.zshrc`):
```bash
echo 'export JAVA_HOME=/opt/homebrew/opt/openjdk@17' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
java -version   # expect 17.x
```

### 3. Android SDK & Platform Tools
Either install **Android Studio** (`brew install --cask android-studio`) or the command-line tools (`brew install android-commandlinetools`). Then set:
```bash
echo 'export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools' >> ~/.zshrc   # Homebrew SDK
# — or, for Android Studio's SDK —
# export ANDROID_HOME="$HOME/Library/Android/sdk"
echo 'export PATH="$ANDROID_HOME/platform-tools:$PATH"' >> ~/.zshrc
source ~/.zshrc
```
Accept licenses once: `yes | sdkmanager --licenses`.

---

## 💻 Working with Android Studio

1. `open -a "Android Studio" .`
2. Open the project root and let Gradle sync (KSP + Compose plugins resolve on first sync).
3. Create an AVD: **Tools ➔ Device Manager ➔ Create Device** (e.g. Pixel 8), system image **API 34** (`arm64-v8a` on Apple Silicon).
4. Run with `Control + R`.

---

## 📱 Local Debug Keystore

The debug build type signs with a custom config that expects `debug.keystore` at the repo root (gitignored). Generate it once:

```bash
keytool -genkeypair -v -keystore debug.keystore -alias androiddebugkey \
  -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Android Debug,O=Android,C=US"
```

---

## 🚀 Terminal Developer Scripts

| Task | Command |
| :--- | :--- |
| **Full macOS environment setup** | `./scripts/setup_mac.sh` |
| **Check environment** | `./scripts/dev_setup.sh` |
| **Run unit & Robolectric tests** | `./scripts/run_tests.sh` |
| **Build debug APK** | `./scripts/build_apk.sh` |
| **Build, install & launch on device** | `./scripts/run_app.sh` |
| **Clean & rebuild** | `./scripts/clean_build.sh` |
| **Build release AAB (Play Store)** | `./scripts/build_release.sh aab` |
| **Build release APK (sideload)** | `./scripts/build_release.sh apk` |
| **Generate release keystore** | `./scripts/generate_release_keystore.sh` |
| **Deploy Firestore rules** | `./scripts/deploy-firestore-rules.sh` |
| **Record Roborazzi screenshots** | `./gradlew :app:recordRoborazziDebug` |
| **Verify Roborazzi screenshots** | `./gradlew :app:verifyRoborazziDebug` |

HTML test report after a run: `open app/build/reports/tests/testDebugUnitTest/index.html`

---

## 🔍 Useful macOS Commands

| Task | Command |
| :--- | :--- |
| **List devices** | `adb devices` |
| **Screenshot** | `adb exec-out screencap -p > screenshot.png` |
| **Record screen** | `adb shell screenrecord /sdcard/demo.mp4 && adb pull /sdcard/demo.mp4 .` |

---

## 🔧 Troubleshooting

See the [Troubleshooting table in DEPLOYMENT.md](DEPLOYMENT.md#-troubleshooting) — the most common macOS issues are keg-only JDK 17 (`JAVA_HOME` export), missing `ANDROID_HOME`, and the gitignored `debug.keystore`.

---

*Happy coding! ⚽ ClubLedger Team*
