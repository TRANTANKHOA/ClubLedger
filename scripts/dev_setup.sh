#!/usr/bin/env bash
# ==============================================================================
# ClubLedger - Local Development Environment Verification Script
# ==============================================================================

set -e

echo "=========================================================="
echo "🚀 ClubLedger: Checking Local Development Environment..."
echo "=========================================================="

# 1. Check Java Version (Requires JDK 17+)
if command -v java >/dev/null 2>&1; then
    JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    echo "✅ Java installed: $(java -version 2>&1 | head -n 1)"
    if [ "$JAVA_VER" -lt 17 ]; then
        echo "⚠️  Warning: JDK 17 or higher is recommended for Android Gradle Plugin 8+."
    fi
else
    echo "❌ Java (JDK 17+) is not found in PATH. Please install OpenJDK 17."
    exit 1
fi

# 2. Check Android SDK
if [ -n "$ANDROID_SDK_ROOT" ] || [ -n "$ANDROID_HOME" ]; then
    SDK_PATH="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
    echo "✅ Android SDK detected at: $SDK_PATH"
else
    echo "⚠️  ANDROID_SDK_ROOT is not set. If using Android Studio, it will auto-configure."
fi

# 3. Check Google Services configuration
if [ -f "app/google-services.json" ]; then
    echo "✅ Firebase configuration found (app/google-services.json)"
else
    echo "ℹ️  Firebase google-services.json not present."
    echo "   The app will run in robust Offline-First Room SQLite mode by default."
    echo "   To enable Cloud Sync on real devices, place your google-services.json in app/"
fi

# 4. Check Gradle wrapper / build configuration
echo "📦 Running debug compilation..."
if command -v ./gradlew >/dev/null 2>&1; then
    ./gradlew compileDebugKotlin
elif command -v gradle >/dev/null 2>&1; then
    gradle compileDebugKotlin
else
    echo "⚠️  Gradle not found in current path. Open project in Android Studio to build."
fi

echo "=========================================================="
echo "🎉 Development environment check complete!"
echo "👉 To run the app:"
echo "   - In Android Studio: Select 'app' run configuration & click Run (Shift+F10)"
echo "   - Via CLI: ./gradlew installDebug"
echo "=========================================================="
