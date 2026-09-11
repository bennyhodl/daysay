# Releasing to Google Play

## One-time setup

1. **Developer account.** Sign up at play.google.com/console. The fee is 25 USD. Personal accounts
   must verify identity and, before a first production release, run a closed test with at least 12
   testers opted in for 14 days.
2. **Upload key.** `build.sh` created one at `~/.daylight-mic/upload-keystore.jks` with its
   passwords in `keystore.properties` in the repo root. Both are ignored by git. Back them up
   somewhere safe. If the key is lost, Play can reset the upload key because Play App Signing holds
   the real signing key.
3. **Create the app** in the Play Console with the listing text from `LISTING.md`, the icon and
   feature graphic from this folder, and at least two screenshots from the DC-1.
4. **App content.** Fill in the privacy policy URL, the accessibility API form, the foreground
   service form, the data safety form, and the content rating. `LISTING.md` has the answers.

## Every release

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`. Play rejects a bundle whose
   `versionCode` is not higher than the last one.
2. Build the signed bundle:

   ```bash
   ./build.sh release
   ```

   Output: `app/build/outputs/bundle/release/app-release.aab`.
3. In the Play Console, open Testing, then Internal testing, create a release, upload the bundle,
   add release notes, and roll out. Install it on the DC-1 from the internal testing link to check
   the release build, which is minified, unlike the debug APK.
4. Promote the release to closed testing, then production, when the checks pass.

## What the release build changes

- Code shrinking and resource shrinking are on. `app/proguard-rules.pro` keeps the JNI bridge.
- The bundle is signed with the upload key. Play re-signs it with the app signing key.
- `targetSdk` is 36, as Play requires for new apps since 31 August 2026.
