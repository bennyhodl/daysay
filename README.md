# Daysay

Push-button dictation for the Daylight DC-1. Press the orange side button once to listen,
press it again to transcribe. The text goes into the focused text field and to the clipboard.
No full-screen keyboard, no Google speech services.

## How it works

One accessibility service does three jobs:

1. It sees every hardware key press before the rest of the system and toggles dictation on the
   trigger key.
2. It draws the small speech bubble as an accessibility overlay. No overlay permission is needed.
3. It acts as a minimal input method, so the transcript is committed straight into the focused
   field through a real input connection. Fallbacks: set-text on the focused node, then paste.

Audio is captured at 16 kHz mono PCM and sent to the model you picked from one list:

- **On device**: Small, Medium, Large, Medium multilingual, Large multilingual. These are
  whisper.cpp models, built from the vendored source in `app/src/main/cpp/whisper.cpp`. They
  download from Hugging Face when you pick them.
- **Remote**, marked with a cloud icon: Groq, OpenAI, or OpenRouter, with your own key. Groq and
  OpenAI use the `/audio/transcriptions` endpoint. OpenRouter sends the audio to a chat model that
  accepts audio.

An optional cleanup pass sends the transcript to a chat model for punctuation and filler removal.
It is off by default and always remote. One API key per provider serves both transcription and
cleanup.

## Build

Requires JDK 17 or later and the Android SDK with `cmdline-tools`.

```bash
./build.sh          # builds the debug APK
./build.sh install  # also installs it on the connected device
./build.sh release  # builds the signed bundle for Google Play
```

The script finds the JDK through `JAVA_HOME`, then Homebrew OpenJDK on macOS, then
`~/.local/jdk` on Linux. It finds the SDK through `ANDROID_HOME`, then `~/Library/Android/sdk`
on macOS, then `~/Android/Sdk` on Linux. It installs the platform, NDK, and CMake versions from
`app/build.gradle.kts` with `sdkmanager` when they are missing.

Install with `adb install`. An APK installed by tapping it in a file manager is marked as
sideloaded, and Android 13 then greys out the accessibility switch until you open
App info, tap the menu in the top right, and choose Allow restricted settings.

## Screens

- **Home**: the setup steps until they are done, then only the live waveform slab, centred on the
  page. The slab names the model in use. Tap it to try a dictation, or press the orange button from
  any app. While Daysay is on screen the floating panel stays hidden; the slab shows the state.
- **History**: every transcript, newest first, with copy and delete.
- **Settings**: the model list, the cleanup pass, and an Advanced section for cleanup
  instructions, remote model ids, speech options, the trigger button, and permissions.

## First run on the DC-1

1. Open Daysay. Grant the microphone permission.
2. Tap Open settings next to Accessibility service and enable Daysay dictation.
3. The orange side button (F11) is the trigger by default. Tap Top button to use the top one
   (F12) instead, or Learn button to record any other key. The Last key seen line shows every
   key the service receives.
4. Pick a model. An on-device model downloads when you tap it. A remote model asks for the
   provider's API key.
5. Focus any text field, press the orange button, speak, press it again.

If the orange button never shows up under Last key seen, it does not reach the Android input
layer. Then use the quick settings tile, or map the button with KeyMapper to the
`Toggle dictation` activity (`dev.bennyb.daysay.TOGGLE`).

## The orange buttons

Measured with `adb shell getevent -lq` on a DC-1:

| Button | Linux key | Scan code | Android key code |
|---|---|---|---|
| Side | `KEY_F11` | 87 | `KEYCODE_F11` (141) |
| Top | `KEY_F12` | 88 | `KEYCODE_F12` (142) |

Both are plain keys that the system does not reserve, so the accessibility service can see and
consume them.

## Google Play

The `play/` folder has the privacy policy, the store listing text with answers for the
accessibility and foreground service declarations, the listing art, and a release checklist in
`play/RELEASE.md`. Release builds are signed with an upload key that stays outside the repo.

## License

MIT. The vendored whisper.cpp and ggml sources in `app/src/main/cpp/whisper.cpp` are MIT
licensed by their authors; see the LICENSE file in that directory.
