# Daylight Mic. Context

## Ubiquitous language

- **Trigger button**: the hardware key that starts and stops dictation. Default is the DC-1
  orange side button, `KEYCODE_F11` with scan code 87. The top button is `KEYCODE_F12`, scan
  code 88. Stored as a key code plus a scan code (`TriggerKey`).
- **Learn mode**: the app state in which the next key press becomes the trigger button.
- **Dictation**: one full cycle: listen, transcribe, optional cleanup, deliver.
- **Listening**: the microphone is open and audio is captured.
- **Engine**: the component that turns audio into text. Two kinds: **on device** (whisper.cpp)
  and **remote** (Groq, OpenAI, OpenRouter with the user's own key).
- **Cleanup pass**: the optional second step that sends the transcript to a chat model to fix
  punctuation and remove fillers. Always remote.
- **Delivery**: how the text reaches the user. `INSERTED` through the input connection or
  set-text, `PASTED` through the clipboard paste action, `CLIPBOARD` when no field has focus.
  The clipboard always receives the text.
- **Bubble**: the floating panel that shows the waveform and the dictation state. Ink on paper inverted.
- **Slab**: the ink block on the home screen. It is the live waveform and the test button in one.
- **History**: the list of past transcripts.
- **Provider**: a remote API vendor. Each has a base URL, a default transcription model, and a
  default chat model. One API key per provider.
- **Local model**: a ggml whisper model file on the device, identified by its catalog id such as
  `base.en-q5_1`.

## Components

| Component | File | Job |
|---|---|---|
| `Dictation` | `core/Dictation.kt` | State machine and pipeline. Single entry point `toggle()`. |
| `DictationAccessibilityService` | `trigger/DictationAccessibilityService.kt` | Key filter, overlay host, input method, text delivery. |
| `Bubble` | `overlay/Bubble.kt` | Overlay view, one per service. |
| `Recorder` | `audio/Recorder.kt` | 16 kHz mono PCM16 capture. |
| `MicForegroundService` | `audio/MicForegroundService.kt` | Foreground service with microphone type while listening. Best effort. |
| `LocalWhisperEngine` | `engine/LocalWhisperEngine.kt` | whisper.cpp through `WhisperLib` JNI. |
| `RemoteTranscriptionEngine` | `engine/RemoteTranscriptionEngine.kt` | Provider HTTP calls. |
| `CleanupClient` | `cleanup/CleanupClient.kt` | Chat completion for the cleanup pass. |
| `ModelManager` | `model/ModelManager.kt` | Model catalog, download, delete. |
| `SettingsStore` | `settings/Settings.kt` | All user settings as one `AppSettings` value. |
| `MainActivity` | `ui/MainActivity.kt` | Permissions, hosts `AppRoot`. |
| `AppRoot` | `ui/AppRoot.kt` | Three screens: Home, History, Settings. Home hides setup once complete. |
| `Waveform`, `WaveformModel`, `WaveformView` | `ui/Waveform.kt`, `overlay/` | One bar model shared by the home slab (Compose) and the floating panel (View). |
| `TranscriptStore` | `model/TranscriptStore.kt` | History of transcripts on disk, newest first. |
| `ToggleActivity`, `DictationTileService` | `trigger/` | Alternative triggers. |

## Rules

- Key handling in `onKeyEvent` must return within 500 ms. Never do work there.
- A consumed key DOWN must be followed by a consumed key UP.
- The whisper context is used from one thread only.
- The bubble never takes focus. It uses `TYPE_ACCESSIBILITY_OVERLAY` with `FLAG_NOT_FOCUSABLE`.
- No colour carries meaning in the UI. Paper `#F3EEE4`, ink `#141414`. Serif for the one big line per
  screen, sans for the rest, monospace for transcripts.
- The floating panel keeps one height in every state. Only Cancel is live while processing.
- Google speech services are not used.

## Release

- `targetSdk` 36, as Google Play requires. `compileSdk` 37.2.
- Release builds are minified. `app/proguard-rules.pro` keeps the JNI bridge.
- The accessibility disclosure dialog is shown before every trip to the accessibility settings.
  It is a Play policy requirement. Keep its text in step with `play/PRIVACY.md`.

## Target

Daylight DC-1. Android 13, API 33, arm64-v8a, MediaTek Helio G99. `minSdk` is 33.
