#!/bin/zsh

# ─────────────────────────────────────────────
#  Android Build & Run Script
#  Works for ANY Android project on this Mac
#  Usage: ./run_android.sh [avd_name]
# ─────────────────────────────────────────────

# ── CONFIG ────────────────────────────────────
ANDROID_HOME="$HOME/Library/Android/sdk"
ANDROID_AVD_HOME="/Volumes/prafull2/.android/avd"
ADB="$ANDROID_HOME/platform-tools/adb"
EMULATOR="$ANDROID_HOME/emulator/emulator"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Use passed AVD name or default to Pixel_9a
AVD_NAME="${1:-Pixel_9a}"

# ── COLORS ────────────────────────────────────
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo "${CYAN}  🚀 Android Build & Run Script${NC}"
echo "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# ── STEP 1: Validate SDK ──────────────────────
echo "\n${YELLOW}[1/5] Checking Android SDK...${NC}"
if [ ! -f "$ADB" ]; then
  echo "${RED}❌ ADB not found at: $ADB${NC}"
  echo "    Make sure Android Studio is installed."
  exit 1
fi
echo "${GREEN}✅ SDK found at: $ANDROID_HOME${NC}"

# ── STEP 2: Export paths for this session ─────
echo "\n${YELLOW}[2/5] Setting up environment...${NC}"
export ANDROID_HOME="$ANDROID_HOME"
export ANDROID_AVD_HOME="$ANDROID_AVD_HOME"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$PATH"
echo "${GREEN}✅ Environment ready${NC}"

# ── STEP 3: Check if emulator already running ─
echo "\n${YELLOW}[3/5] Checking for running emulator...${NC}"
RUNNING=$("$ADB" devices | grep "emulator" | grep "device")

if [ -n "$RUNNING" ]; then
  echo "${GREEN}✅ Emulator already running: $RUNNING${NC}"
else
  echo "   No emulator found. Launching: ${CYAN}$AVD_NAME${NC}"

  # Check AVD exists
  AVDS=$("$EMULATOR" -list-avds 2>/dev/null)
  if ! echo "$AVDS" | grep -q "$AVD_NAME"; then
    echo "${RED}❌ AVD '$AVD_NAME' not found!${NC}"
    echo "   Available AVDs:"
    echo "$AVDS" | sed 's/^/   - /'
    echo "\n   Usage: ./run_android.sh <avd_name>"
    exit 1
  fi

  # Launch emulator in background
  "$EMULATOR" -avd "$AVD_NAME" -no-snapshot-load &>/dev/null &
  EMULATOR_PID=$!
  echo "   Emulator PID: $EMULATOR_PID"

  # ── STEP 4: Wait for emulator to fully boot ─
  echo "\n${YELLOW}[4/5] Waiting for emulator to boot (this takes ~60s)...${NC}"
  "$ADB" wait-for-device

  # Wait until boot animation is complete
  BOOT_STATUS=""
  WAIT=0
  until [[ "$BOOT_STATUS" == "1" ]]; do
    BOOT_STATUS=$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
    if [[ "$BOOT_STATUS" != "1" ]]; then
      printf "."
      sleep 3
      WAIT=$((WAIT + 3))
      if [ $WAIT -gt 180 ]; then
        echo "\n${RED}❌ Emulator boot timed out after 3 minutes${NC}"
        exit 1
      fi
    fi
  done
  echo "\n${GREEN}✅ Emulator fully booted!${NC}"
fi

# ── STEP 5: Build & Install ───────────────────
echo "\n${YELLOW}[5/5] Building and installing app...${NC}"
cd "$PROJECT_DIR"

if [ ! -f "./gradlew" ]; then
  echo "${RED}❌ gradlew not found in: $PROJECT_DIR${NC}"
  echo "   Make sure you run this script from your Android project root."
  exit 1
fi

chmod +x ./gradlew
./gradlew installDebug

if [ $? -eq 0 ]; then
  echo "\n${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo "${GREEN}  ✅ App installed successfully!${NC}"
  echo "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

  # ── Auto-launch the app ─────────────────────
  echo "\n🚀 Launching app on emulator..."
  PACKAGE=$(grep 'package=' app/src/main/AndroidManifest.xml | head -1 | sed 's/.*package="\([^"]*\)".*/\1/')
  ACTIVITY="${PACKAGE}.MainActivity"
  "$ADB" shell am start -n "${PACKAGE}/.MainActivity" 2>/dev/null
  echo "${GREEN}✅ App launched: $PACKAGE${NC}"
else
  echo "\n${RED}❌ Build failed! Check errors above.${NC}"
  exit 1
fi
