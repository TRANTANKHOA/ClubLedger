#!/bin/bash
# ==============================================================================
# ClubLedger - Build Debug APK (Local macOS / Unix)
# ==============================================================================

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${CYAN}${BOLD}⚽ Compiling ClubLedger Debug APK...${NC}\n"

if [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
else
    GRADLE_CMD="gradle"
fi

$GRADLE_CMD assembleDebug "$@"

echo -e "\n${GREEN}${BOLD}✓ APK Build Successful!${NC}"
echo -e "Output Location: ${CYAN}app/build/outputs/apk/debug/app-debug.apk${NC}"
echo -e "Reveal in Finder: ${BOLD}open -R app/build/outputs/apk/debug/app-debug.apk${NC}\n"
