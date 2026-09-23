#!/bin/bash
# ==============================================================================
# ClubLedger - macOS Local Development Setup Script
# Works on macOS (Apple Silicon M1/M2/M3/M4 and Intel x86_64)
# ==============================================================================

set -e

# ANSI Color Codes
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m' # No Color

echo -e "${CYAN}${BOLD}"
echo "============================================================"
echo "   ⚽ ClubLedger - macOS Environment Setup"
echo "============================================================"
echo -e "${NC}"

# Detect macOS Architecture
ARCH=$(uname -m)
echo -e "🍏 ${BOLD}Detected System:${NC} macOS ($ARCH)"

# 1. Check for Homebrew
echo -e "\n${CYAN}[1/5] Checking Homebrew...${NC}"
if ! command -v brew >/dev/null 2>&1; then
    echo -e "${YELLOW}Homebrew is not installed. Installing Homebrew...${NC}"
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

    # Add Homebrew to PATH depending on architecture
    if [ "$ARCH" = "arm64" ]; then
        echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
        eval "$(/opt/homebrew/bin/brew shellenv)"
    else
        echo 'eval "$(/usr/local/bin/brew shellenv)"' >> ~/.zprofile
        eval "$(/usr/local/bin/brew shellenv)"
    fi
else
    echo -e "${GREEN}✓ Homebrew is installed.${NC}"
fi

# 2. Check and Install OpenJDK 17
echo -e "\n${CYAN}[2/5] Checking Java (JDK 17)...${NC}"
JAVA_NEEDS_INSTALL=false

if command -v java >/dev/null 2>&1; then
    JAVA_VER=$(java -version 2>&1 | head -n 1)
    echo -e "Found Java: ${JAVA_VER}"
    if echo "$JAVA_VER" | grep -q '17'; then
        echo -e "${GREEN}✓ Java version is compatible.${NC}"
    else
        echo -e "${YELLOW}Java version is not 17. Installing OpenJDK 17...${NC}"
        JAVA_NEEDS_INSTALL=true
    fi
else
    echo -e "${YELLOW}Java is not installed. Installing OpenJDK 17...${NC}"
    JAVA_NEEDS_INSTALL=true
fi

if [ "$JAVA_NEEDS_INSTALL" = true ]; then
    brew install openjdk@17
    sudo ln -sfn $(brew --prefix openjdk@17)/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk || true
    echo 'export JAVA_HOME=/opt/homebrew/opt/openjdk@17' >> ~/.zshrc
    echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
    export JAVA_HOME=/opt/homebrew/opt/openjdk@17
    export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
fi

# 3. Check / Configure Android SDK Environment
echo -e "\n${CYAN}[3/5] Configuring Android SDK & Environment Variables...${NC}"
STUDIO_ANDROID_HOME="$HOME/Library/Android/sdk"
BREW_ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"

if [ -d "$STUDIO_ANDROID_HOME" ]; then
    export ANDROID_HOME="$STUDIO_ANDROID_HOME"
    export ANDROID_SDK_ROOT="$STUDIO_ANDROID_HOME"
    echo -e "${GREEN}✓ Android SDK found at: $ANDROID_HOME${NC}"
elif [ -d "$BREW_ANDROID_HOME" ]; then
    export ANDROID_HOME="$BREW_ANDROID_HOME"
    export ANDROID_SDK_ROOT="$BREW_ANDROID_HOME"
    echo -e "${GREEN}✓ Android SDK found at: $ANDROID_HOME (Homebrew commandlinetools)${NC}"
elif [ -n "$ANDROID_HOME" ]; then
    echo -e "${GREEN}✓ ANDROID_HOME is set: $ANDROID_HOME${NC}"
else
    echo -e "${YELLOW}Android SDK not found at $STUDIO_ANDROID_HOME or $BREW_ANDROID_HOME.${NC}"
    echo -e "If you have not installed Android Studio yet, you can install it via Homebrew:"
    echo -e "    ${BOLD}brew install --cask android-studio${NC}"
    echo -e "Or the command-line tools only: ${BOLD}brew install android-commandlinetools${NC}"
fi

# Check for platform-tools (adb)
if ! command -v adb >/dev/null 2>&1; then
    if [ -d "$STUDIO_ANDROID_HOME/platform-tools" ]; then
        export PATH="$STUDIO_ANDROID_HOME/platform-tools:$PATH"
    elif [ -d "$BREW_ANDROID_HOME/platform-tools" ]; then
        export PATH="$BREW_ANDROID_HOME/platform-tools:$PATH"
    else
        echo -e "${YELLOW}Installing android-platform-tools via Homebrew for 'adb'...${NC}"
        brew install --cask android-platform-tools || true
    fi
fi

# 4. Verify environment via the repo's own checker
echo -e "\n${CYAN}[4/5] Verifying environment (scripts/dev_setup.sh)...${NC}"
if [ -f "scripts/dev_setup.sh" ]; then
    chmod +x scripts/dev_setup.sh
    ./scripts/dev_setup.sh || true
fi

# 5. Set executable permissions on scripts
echo -e "\n${CYAN}[5/5] Setting executable permissions on project scripts...${NC}"
chmod +x gradlew || true
chmod +x scripts/*.sh 2>/dev/null || true

echo -e "${GREEN}${BOLD}============================================================"
echo -e "   🎉 macOS Local Environment Setup Completed!"
echo -e "============================================================${NC}"
echo -e "\nQuick command reference for macOS terminal:"
echo -e "  • Run unit tests:                 ${CYAN}./scripts/run_tests.sh${NC}"
echo -e "  • Build Debug APK:                ${CYAN}./scripts/build_apk.sh${NC}"
echo -e "  • Run on connected device/AVD:    ${CYAN}./scripts/run_app.sh${NC}"
echo -e "  • Clean & rebuild:                ${CYAN}./scripts/clean_build.sh${NC}"
echo -e "  • Build release AAB/APK:          ${CYAN}./scripts/build_release.sh aab${NC}"
echo -e "\nTo open the project in Android Studio on macOS:"
echo -e "  ${BOLD}open -a \"Android Studio\" .${NC}\n"
