# Install Daysay

Daysay targets the Daylight DC-1. You can install a built APK with Android Debug Bridge, or build
the app from source.

## Device requirements

- Daylight DC-1 or another arm64 Android device
- Android 13 or later, API level 33 or later
- A microphone
- Free storage for the app and one local model
- An internet connection for the initial model download

The local model choices use approximately this much storage:

| Model | Language | Download size |
|---|---|---:|
| Small | English | 32 MB |
| Medium | English | 60 MB |
| Large | English | 190 MB |
| Medium multilingual | Multiple languages | 60 MB |
| Large multilingual | Multiple languages | 190 MB |
| Parakeet | European languages | 640 MB |

Allow extra free space while a model downloads. After the download, local transcription works
without an internet connection.

## Install an APK

This method requires Android Debug Bridge, also called `adb`, and USB debugging on the tablet.

1. Connect the tablet to your computer and accept the USB debugging prompt.
2. Confirm that `adb` can see it:

   ```bash
   adb devices
   ```

3. Install or update Daysay:

   ```bash
   adb install -r daysay.apk
   ```

4. Open Daysay and complete the permission setup.
5. Select a local model and wait for its download to finish.

Android 13 can restrict accessibility services for sideloaded apps. If the Daysay accessibility
switch is unavailable, open the Daysay App info screen. Open the top-right menu, select
**Allow restricted settings**, and then enable the service again.

## Required permissions

Daysay requests only the access needed for dictation:

- **Microphone** records speech during Listening.
- **Accessibility service** keeps dictation available over other apps and delivers the transcript
  to the focused text field.
- **Notifications** lets Android show the required microphone foreground-service notification.
- **Internet** downloads local models and connects to a remote provider when you select one.

The accessibility service reads only the focused text field and cursor position when it delivers
a transcript. It does not store the field contents. The clipboard always receives the completed
transcript, including when no editable field has focus.

## Build from source

You need:

- Git
- JDK 17 or later
- Android SDK command-line tools
- `adb` for installation on a connected tablet
- An internet connection for Gradle dependencies and Android SDK packages

Clone the repository and build the debug APK:

```bash
git clone https://github.com/bennyhodl/daysay.git
cd daysay
./build.sh
```

The build script locates `JAVA_HOME` and `ANDROID_HOME`. It installs the required Android platform,
NDK, and CMake versions with `sdkmanager` if they are missing. The current build uses Android SDK
37.2, NDK 28.2.13676358, and CMake 3.31.6.

The APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

To build and install it on a connected device in one step, run:

```bash
./build.sh install
```

## First setup

1. Grant microphone and notification access.
2. Enable the Daysay accessibility service.
3. Select a model.
4. Wait until a local model shows that its download is complete.
5. Focus an editable field in any app and start a dictation.

Remote models are optional. They require an API key for Groq, OpenAI, or OpenRouter. The optional
cleanup pass also uses the selected remote provider.

## Troubleshooting

### The accessibility switch is unavailable

Allow restricted settings from the Daysay App info menu, then return to Accessibility settings.

### `adb` does not list the tablet

Confirm that USB debugging is enabled, reconnect the cable, and accept the authorization prompt
on the tablet. Run `adb devices` again.

### A local model does not download

Check the network connection and available storage. A partial download does not become the active
model. Start the download again after you correct the problem.

### Text does not appear in the focused field

Check that the accessibility service is enabled and that the target field accepts text. Daysay
also copies the transcript to the clipboard, so you can paste it manually.
