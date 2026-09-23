#!/bin/bash
# ==============================================================================
# ClubLedger - Build, Install & Launch on Android Device / Mac Emulator
# ==============================================================================

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m'

PACKAGE_NAME="com.aistudio.clubledger.sports"
MAIN_ACTIVITY="com.example.MainActivity"

echo -e "${CYAN}${BOLD}⚽ Installing & Running ClubLedger on Android Target...${NC}\n"

# Check for ADB (Android Studio SDK or Homebrew commandlinetools)
if ! command -v adb >/dev/null 2>&1; then
    for TOOLS_DIR in "$HOME/Library/Android/sdk/platform-tools" "/opt/homebrew/share/android-commandlinetools/platform-tools"; do
        if [ -d "$TOOLS_DIR" ]; then
            export PATH="$TOOLS_DIR:$PATH"
            break
        fi
    done
    command -v adb >/dev/null 2>&1 || echo -e "${YELLOW}Warning: 'adb' not found in PATH. Please ensure Android platform-tools is installed.${NC}"
fi

# Check connected devices
DEVICE_COUNT=$(adb devices 2>/dev/null | grep -v "List of devices" | grep -c "device$" || true)

if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo -e "${YELLOW}No Android devices or emulators detected via ADB.${NC}"
    echo -e "Please start an Android Virtual Device (AVD) in Android Studio, or connect a physical phone via USB with USB Debugging enabled."
    echo -e "Building APK now regardless...\n"
fi

if [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
else
    GRADLE_CMD="gradle"
fi

if [ "$DEVICE_COUNT" -gt 0 ]; then
    $GRADLE_CMD installDebug
    echo -e "\n${CYAN}Launching MainActivity on device...${NC}"
    adb shell am start -n "$PACKAGE_NAME/$MAIN_ACTIVITY"
    echo -e "${GREEN}${BOLD}✓ ClubLedger is now running on your device/emulator!${NC}\n"
else
    $GRADLE_CMD assembleDebug
    echo -e "${GREEN}${BOLD}✓ APK built. Connect a device or start an emulator, then run ./scripts/run_app.sh again.${NC}\n"
fi
