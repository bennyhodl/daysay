# Daylight Mic privacy policy

Last updated: 11 September 2026

Daylight Mic is a dictation app for Android. It is made by Ben Schroth. This policy explains what
the app does with your data.

## What the app collects

**Audio.** When you start a dictation, the app records from the microphone until you stop it. The
recording is held in memory only for as long as it takes to turn it into text. It is not saved to
disk and it is not kept after the transcript is produced.

**Transcripts.** The text of each dictation is stored on your device so you can see it again in the
History screen. You can delete any transcript, or all of them, at any time. Transcripts never leave
the device unless you choose the cleanup pass described below.

**Settings.** Your engine choice, API keys, and preferences are stored on the device in the app's
private storage. API keys are not sent anywhere except to the provider they belong to.

**The focused text field.** To insert a transcript, the app's accessibility service reads the text
and cursor position of the text field that has focus at that moment, and writes the transcript into
it. It does not read other parts of the screen, it does not record what you type, and it does not
store or transmit the contents of any field.

**Hardware keys.** The accessibility service sees hardware key presses so the orange button can
start and stop dictation. Key presses are matched against the trigger key and then discarded. The
app does not log or transmit key presses.

## What leaves the device

Nothing leaves the device when you use the on-device engine with the cleanup pass off. This is the
default.

If you choose a remote engine, your recorded audio is sent over an encrypted connection to the
provider you selected, using the API key you entered: Groq (groq.com), OpenAI (openai.com), or
OpenRouter (openrouter.ai). The provider returns the transcript. If you turn on the cleanup pass,
the transcript text is sent to the provider you selected for cleanup. Each provider handles that
data under its own privacy policy and terms.

Model files for the on-device engine are downloaded from Hugging Face (huggingface.co) when you
request them. That request contains no personal data.

## What the app does not do

- It does not collect analytics, crash reports, advertising identifiers, or usage statistics.
- It does not create an account or ask you to sign in.
- It does not share or sell data to anyone.
- It does not read your screen, contacts, location, files, or messages.

## Permissions

- **Microphone**: to record your speech.
- **Accessibility service**: to see the hardware button, show the dictation panel, and insert text
  into the focused field. This is disclosed inside the app before you turn it on.
- **Notifications**: to show a persistent notification while a dictation is in progress.
- **Internet**: only for remote engines, the cleanup pass, and model downloads.

## Your choices

You can switch engines, turn the cleanup pass off, remove API keys, delete transcripts, and turn
off the accessibility service at any time from the app or from Android settings. Uninstalling the
app removes all data it stored.

## Contact

Questions about this policy: ben@bitcoinbay.foundation
