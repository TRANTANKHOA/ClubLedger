#!/bin/bash
# ==============================================================================
# ClubLedger - Clean & Rebuild (Local macOS / Unix)
# ==============================================================================

set -e

CYAN='\033[0;36m'
GREEN='\033[0;32m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${CYAN}${BOLD}⚽ Cleaning & Rebuilding ClubLedger...${NC}\n"

if [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
else
    GRADLE_CMD="gradle"
fi

$GRADLE_CMD clean assembleDebug

echo -e "\n${GREEN}${BOLD}✓ Clean build completed successfully!${NC}\n"
