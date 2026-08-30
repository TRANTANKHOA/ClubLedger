#!/usr/bin/env bash
# ==============================================================================
# ClubLedger - Production Release Build Script
# ==============================================================================

set -e

BUILD_TYPE="${1:-aab}" # default to Android App Bundle (aab), or 'apk'

echo "=========================================================="
echo "🏗️  Building ClubLedger Production Release Artifact ($BUILD_TYPE)..."
echo "=========================================================="

GRADLE_CMD="./gradlew"
if ! command -v ./gradlew >/dev/null 2>&1; then
    if command -v gradle >/dev/null 2>&1; then
        GRADLE_CMD="gradle"
    else
        echo "❌ Gradle not found. Please ensure Gradle is installed or use Android Studio."
        exit 1
    fi
fi

# Run pre-build verification and tests
echo "🧪 Running unit and Robolectric tests..."
$GRADLE_CMD testDebugUnitTest

if [ "$BUILD_TYPE" = "apk" ]; then
    echo "📦 Assembling Release APK..."
    $GRADLE_CMD :app:assembleRelease
    echo "✅ Release APK generated:"
    echo "   Location: app/build/outputs/apk/release/app-release.apk (or app-release-unsigned.apk)"
else
    echo "📦 Building Release Android App Bundle (AAB) for Google Play..."
    $GRADLE_CMD :app:bundleRelease
    echo "✅ Release AAB generated:"
    echo "   Location: app/build/outputs/bundle/release/app-release.aab"
fi

echo "=========================================================="
echo "🎉 Build completed successfully!"
echo "=========================================================="
