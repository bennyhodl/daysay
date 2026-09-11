# Daylight Mic

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

Audio is captured at 16 kHz mono PCM and sent to one of two engines:

- **On device**: whisper.cpp, built from the vendored source in `app/src/main/cpp/whisper.cpp`.
  Models download from Hugging Face on first use.
- **Remote API** with your own key: Groq, OpenAI, or OpenRouter. Groq and OpenAI use the
  `/audio/transcriptions` endpoint. OpenRouter sends the audio to a chat model that accepts audio.

An optional cleanup pass sends the transcript to a chat model for punctuation and filler removal.
It is off by default and always remote.

## Build

Requires JDK 17 or 21, the Android SDK with platform 36, build-tools 36.0.0, NDK 28.2.13676358,
and CMake 3.31.6. Set `sdk.dir` in `local.properties`.

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Install with `adb install`. An APK installed by tapping it in a file manager is marked as
sideloaded, and Android 13 then greys out the accessibility switch until you open
App info, tap the menu in the top right, and choose Allow restricted settings.

## Screens

- **Home**: the setup steps until they are done, then only the live waveform slab. Tap the slab to try a
  dictation, or press the orange button from any app. The last transcript sits below it.
- **History**: every transcript, newest first, with copy and delete.
- **Settings**: engine and models, the optional cleanup pass, speech options, and an Advanced section for
  the trigger button and permissions.

## First run on the DC-1

1. Open Daylight Mic. Grant the microphone permission.
2. Tap Open settings next to Accessibility service and enable Daylight Mic dictation.
3. The orange side button (F11) is the trigger by default. Tap Top button to use the top one
   (F12) instead, or Learn button to record any other key. The Last key seen line shows every
   key the service receives.
4. Pick an engine. For on device, download a model. For remote, pick a provider and paste a key.
5. Focus any text field, press the orange button, speak, press it again.

If the orange button never shows up under Last key seen, it does not reach the Android input
layer. Then use the quick settings tile, or map the button with KeyMapper to the
`Toggle dictation` activity (`com.benschroth.daylightmic.TOGGLE`).

## The orange buttons

Measured with `adb shell getevent -lq` on a DC-1:

| Button | Linux key | Scan code | Android key code |
|---|---|---|---|
| Side | `KEY_F11` | 87 | `KEYCODE_F11` (141) |
| Top | `KEY_F12` | 88 | `KEYCODE_F12` (142) |

Both are plain keys that the system does not reserve, so the accessibility service can see and
consume them.

## License

MIT. The vendored whisper.cpp and ggml sources in `app/src/main/cpp/whisper.cpp` are MIT
licensed by their authors; see the LICENSE file in that directory.
