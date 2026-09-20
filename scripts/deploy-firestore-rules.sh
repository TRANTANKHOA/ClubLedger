#!/bin/bash
# ==============================================================================
# ClubLedger - Deploy Firestore Security Rules via Firebase CLI
# ==============================================================================

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${CYAN}${BOLD}🔥 Deploying ClubLedger Firestore Security Rules...${NC}\n"

if ! command -v firebase &> /dev/null; then
    echo -e "${YELLOW}Firebase CLI is not installed.${NC}"
    echo -e "Install with: ${BOLD}npm install -g firebase-tools${NC}"
    echo -e "Then login with: ${BOLD}firebase login${NC}"
    exit 1
fi

if [ ! -f "firestore.rules" ]; then
    echo -e "Error: firestore.rules file not found!"
    exit 1
fi

echo -e "Deploying rules to Firebase project..."
firebase deploy --only firestore:rules

echo -e "\n${GREEN}${BOLD}✓ Firestore Security Rules deployed successfully!${NC}\n"
