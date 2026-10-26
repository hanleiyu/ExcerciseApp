#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
if [[ ! -d "$ANDROID_HOME/platforms/android-35" ]]; then
  echo "Android SDK platform 35 not found at $ANDROID_HOME" >&2
  exit 1
fi
./gradlew --no-daemon test assembleDebug
mkdir -p artifacts
cp app/build/outputs/apk/debug/app-debug.apk artifacts/exercise-tracker.apk
echo "APK: $ROOT/artifacts/exercise-tracker.apk"
