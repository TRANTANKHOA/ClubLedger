#!/usr/bin/env bash
# ==============================================================================
# ClubLedger - Test Execution Script
# ==============================================================================

set -e

GRADLE_CMD="./gradlew"
if ! command -v ./gradlew >/dev/null 2>&1; then
    if command -v gradle >/dev/null 2>&1; then
        GRADLE_CMD="gradle"
    else
        echo "❌ Gradle not found."
        exit 1
    fi
fi

echo "=========================================================="
echo "🧪 Running ClubLedger Unit & Robolectric Tests..."
echo "=========================================================="

$GRADLE_CMD :app:testDebugUnitTest --info

echo "=========================================================="
echo "✅ All tests passed successfully!"
echo "=========================================================="
