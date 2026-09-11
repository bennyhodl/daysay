#!/usr/bin/env bash
# Builds the debug APK. Pass "install" to also install it on the connected device.
# Pass "release" to build the signed Android App Bundle for Google Play (needs keystore.properties).
set -euo pipefail
cd "$(dirname "$0")"

# JDK: JAVA_HOME if set, then Homebrew OpenJDK on macOS, then the Linux install.
if [[ -z "${JAVA_HOME:-}" ]]; then
  for dir in \
    /opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home \
    /usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home \
    "$HOME/.local/jdk/jdk-21.0.12.1+1"; do
    [[ -d "$dir" ]] && { JAVA_HOME="$dir"; break; }
  done
fi
export JAVA_HOME

# Android SDK: ANDROID_HOME if set, then the macOS location, then the Linux one.
if [[ -z "${ANDROID_HOME:-}" ]]; then
  for dir in "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
    [[ -d "$dir" ]] && { ANDROID_HOME="$dir"; break; }
  done
fi
export ANDROID_HOME
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

# SDK packages that Gradle does not download by itself. Versions come from app/build.gradle.kts.
ndk=$(sed -n 's/.*ndkVersion = "\(.*\)".*/\1/p' app/build.gradle.kts)
cmake=$(sed -n 's/.*version = "\(.*\)".*/\1/p' app/build.gradle.kts)
sdk=$(sed -n 's/.*compileSdk = \([0-9]*\).*/\1/p' app/build.gradle.kts)
sdk_minor=$(sed -n 's/.*compileSdkMinor = \([0-9]*\).*/\1/p' app/build.gradle.kts)
platform="android-$sdk${sdk_minor:+.$sdk_minor}"
missing=()
[[ -d "$ANDROID_HOME/ndk/$ndk" ]] || missing+=("ndk;$ndk")
[[ -d "$ANDROID_HOME/cmake/$cmake" ]] || missing+=("cmake;$cmake")
[[ -d "$ANDROID_HOME/platforms/$platform" ]] || missing+=("platforms;$platform")
if (( ${#missing[@]} )); then
  echo "Installing: ${missing[*]}"
  "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "${missing[@]}"
fi

case "${1:-}" in
  release)
    [[ -f keystore.properties ]] || { echo "keystore.properties is missing. See play/RELEASE.md."; exit 1; }
    ./gradlew bundleRelease
    echo "AAB: app/build/outputs/bundle/release/app-release.aab"
    ;;
  release-apk)
    [[ -f keystore.properties ]] || { echo "keystore.properties is missing. See play/RELEASE.md."; exit 1; }
    ./gradlew assembleRelease
    echo "APK: app/build/outputs/apk/release/app-release.apk"
    ;;
  *)
    ./gradlew assembleDebug
    echo "APK: app/build/outputs/apk/debug/app-debug.apk"
    if [[ "${1:-}" == "install" ]]; then
      adb install -r app/build/outputs/apk/debug/app-debug.apk
    fi
    ;;
esac
