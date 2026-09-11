#!/usr/bin/env bash
# Builds the debug APK. Pass "install" to also install it on the connected device.
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-$HOME/.local/jdk/jdk-21.0.12.1+1}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
./gradlew assembleDebug
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
if [[ "${1:-}" == "install" ]]; then
  adb install -r app/build/outputs/apk/debug/app-debug.apk
fi
