#!/bin/bash
# ==============================================================================
# ClubLedger - Production Release Keystore Generator
# ==============================================================================

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m'

KEYSTORE_FILE="release.keystore"
KEY_ALIAS="clubledger"

echo -e "${CYAN}${BOLD}🔐 ClubLedger - Generating Production Release Keystore${NC}\n"

if ! command -v keytool >/dev/null 2>&1; then
    echo -e "${YELLOW}keytool not found — install JDK 17 first (brew install openjdk@17).${NC}"
    exit 1
fi

if [ -f "$KEYSTORE_FILE" ]; then
    echo -e "${YELLOW}⚠️ '$KEYSTORE_FILE' already exists in the current directory.${NC}"
    read -p "Do you want to overwrite it? (y/N): " confirm
    if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
        echo "Aborting keystore generation."
        exit 0
    fi
    rm "$KEYSTORE_FILE"
fi

echo -e "Generating 2048-bit RSA Keystore valid for 25 years (10,000 days)..."
echo -e "${YELLOW}Note: development default password 'clubledger2026' — regenerate with your own for production signing.${NC}\n"
keytool -genkeypair \
    -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "clubledger2026" \
    -keypass "clubledger2026" \
    -dname "CN=ClubLedger, OU=Mobile, O=ClubLedger, L=San Francisco, ST=CA, C=US"

echo -e "\n${GREEN}${BOLD}✓ Keystore generated successfully: ${KEYSTORE_FILE}${NC}\n"

echo -e "${CYAN}${BOLD}📋 Certificate Fingerprints (Required for Firebase & Google Sign-In):${NC}"
keytool -list -v -keystore "$KEYSTORE_FILE" -alias "$KEY_ALIAS" -storepass "clubledger2026" | grep -E "SHA1|SHA256"

echo -e "${YELLOW}${BOLD}👉 NEXT STEP:${NC}"
echo -e "1. Copy the SHA-1 and SHA-256 fingerprints above."
echo -e "2. Go to Firebase Console -> Project Settings -> Your Android App -> Add Fingerprint."
echo -e "3. Download the updated 'google-services.json' and place it in the 'app/' directory."
echo -e "4. Export KEYSTORE_PATH/STORE_PASSWORD/KEY_PASSWORD (see DEPLOYMENT.md §4) before release builds."
