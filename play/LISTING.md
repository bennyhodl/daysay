# Google Play listing and declarations

Copy these into the Play Console. Keep them in sync with the app.

## Store listing

**App name**: Daylight Mic

**Short description** (80 characters max):
Press a button, speak, press again. Your words land in the text field.

**Full description**:
Daylight Mic is push-button dictation for the Daylight DC-1 tablet, and any Android device with a
spare hardware key.

Press the orange side button once to start listening. Speak. Press it again, and the transcript is
typed into whatever text field has focus. It is also copied to the clipboard. No full-screen
keyboard takes over, and no Google speech service is involved.

Choose how speech becomes text:
• On device. Whisper runs on the tablet and nothing leaves it. Models download once.
• Remote, with your own key. Groq, OpenAI, or OpenRouter. Fast and accurate, needs Wi-Fi.

Optional cleanup pass: a chat model fixes punctuation and removes filler words before the text is
inserted. Off by default.

Every transcript is kept in History on the device, where you can copy or delete it.

Grayscale design made for the Daylight display. Ink on paper, no colour, no clutter.

This app uses the Android accessibility service to see the hardware button, show the small
dictation panel, and insert text into the focused field. It does not read your screen or your
keystrokes. Full details are shown in the app before you turn the service on.

**App category**: Productivity

**Tags**: dictation, speech to text, voice typing, whisper

**Contact email**: ben@bitcoinbay.foundation

**Privacy policy URL**: https://github.com/bennyhodl/daylight-mic/blob/main/play/PRIVACY.md

**Graphics**: `icon-512.png` (app icon), `feature-graphic-1024x500.png` (feature graphic). Take at
least two screenshots on the DC-1: the Home screen with the ink slab, and the floating panel over a
text app while listening. Play accepts PNG or JPEG, 16:9 or 9:16, at least 320 px on the short side.
Tablet screenshots (7 inch and 10 inch) are separate upload slots; the DC-1 counts as 10 inch.

## App content declarations

### Accessibility API usage form

Answer "No" to "Is your app an accessibility tool". The service is not for people with
disabilities and the manifest sets `isAccessibilityTool="false"`.

**Why the app needs the AccessibilityService API**:
Daylight Mic is a dictation tool triggered by a hardware button. The accessibility service is used
for three narrow purposes: (1) `onKeyEvent` to detect the configured hardware key (by default the
Daylight DC-1 side button, KEYCODE_F11) that starts and stops a dictation; (2) drawing a small
status overlay (TYPE_ACCESSIBILITY_OVERLAY) that shows the listening and transcribing state; (3)
inserting the resulting transcript into the focused editable field through the accessibility input
connection, or ACTION_SET_TEXT and ACTION_PASTE as fallbacks. The service requests only the
focused input node at the moment of insertion. It does not traverse window content, does not
process accessibility events (onAccessibilityEvent is empty), and does not log or transmit key
events or screen content. Automation is deterministic and user-initiated: one button press starts
listening, the next press inserts text.

**Prominent disclosure**: shown in-app in a dialog before the user is sent to the accessibility
settings, from both the Home setup card and Settings. It describes the three uses, the data read
(the focused field's text and cursor), and what is not done. The user must tap "Agree and open
settings".

**Video**: record a short screen capture on the DC-1 showing: open the app, tap "Open settings",
the disclosure dialog, agree, enable the service, then in another app press the side button, speak,
press again, and the text appears in the field.

### Foreground service permissions form

Type declared: **microphone**.

**Description of the feature**: A dictation session is started by a hardware button while any app
is in the foreground. The app runs a microphone foreground service for the duration of the
recording, typically 5 to 60 seconds, so the recording continues reliably while the user's own app
stays on screen, and so the microphone indicator and persistent notification are shown. The service
stops as soon as the user presses the button again.

**Use case**: Voice recording / capture audio input for transcription.

**Video**: the same capture as above shows the notification during listening.

### Data safety form

- Does the app collect or share user data: **Yes** (audio and transcript text can be sent to a
  third-party provider the user configures, optionally).
- Data types:
  - **Audio: Voice or sound recordings**. Collected: yes (processed ephemerally). Shared: yes, with
    the AI provider the user selected, only when a remote engine is chosen. Optional: yes. Purpose:
    app functionality. Encrypted in transit: yes. User can request deletion: not applicable, not
    stored by the developer.
  - **Personal info: Other (transcript text)**. Collected: stored on device only. Shared: yes, with
    the provider, only when the cleanup pass is on. Optional: yes. Purpose: app functionality.
- Data is not collected by the developer. No analytics, no crash reporting, no ads.
- Security practices: data encrypted in transit (HTTPS). Users can delete data from the app.

### Other forms

- **Ads**: no.
- **Content rating**: complete the IARC questionnaire; the app has no user-generated content shared
  with others, no violence, no purchases. Expect "Everyone".
- **Target audience**: 18 and over, or 13 and over. Not designed for children.
- **Government apps / financial features / health**: no.
- **News**: no.
