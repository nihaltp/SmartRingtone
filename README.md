# Smart Ringtone 🎵

A smart, automated Android utility that dynamically changes and escalates contact ringtones based on call behavior (missed, rejected, or answered calls). Built with Jetpack Compose and Kotlin, it integrates directly with the Android Contacts Provider to safely manage per-contact custom ringtones.

---

## 💡 The Core Concept: Call-Score Escalation

The app assigns a **Call Score** (starting at `0`) to each contact. When contacts call you, their score changes based on your response:

* **Missed Call**: Increases the contact's score by **`+1`** 📞
* **Rejected Call**: Increases the contact's score by **`+2`** 🚫
* **Answered Call (Incoming or Outgoing)**: Resets the contact's score back to **`0`** ✅

### Ringtone Mapping

You configure a ordered list (sequence) of custom ringtones in the app. As a contact's Call Score escalates, their custom ringtone shifts through this sequence:

* **Score = 0**: Restores their original custom ringtone or system default.
* **Score >= 1**: Maps to the corresponding ringtone in your configured sequence (`index = score - 1`). If the score exceeds the list length, it caps at the final (often loudest/most urgent) ringtone in the sequence.

This escalation system ensures that contacts trying to reach you repeatedly receive louder, more attention-grabbing ringtones until you answer or call them back, at which point their ringtone resets to default.

---

## ✨ Features

* **Ringtone Sequence Customizer**: Add, delete, and reorder (move up/down) custom audio files from your device. Toggle playback to preview ringtones directly from the UI.
* **Contact Manager**: List all device contacts, search using a multi-term (space-separated) keyword algorithm, and sort them by Name or Call Score.
* **Score Override / Reset**: Manually reset the score of an individual contact or all contacts globally back to `0`, restoring their default ringtones.
* **Tracked Call History (Logs)**: View a local log of calls processed by the background sync, tracking the call direction (incoming/outgoing), type (answered/missed/rejected), timestamp, and the exact score change.
* **Screenshot/Privacy Mode**: Includes a privacy toggle in settings that mocks all contact names and numbers (e.g., Alice Vance, Bob Miller, Charlie Ross) while preserving settings, ideal for taking app screenshots or demonstration.
* **Robust Diagnostics**: Turn on logging to capture errors and background events locally. Review logs in the built-in system log viewer, clear logs, or copy them to the clipboard to report bugs.
* **Progressive Sync Notifications**: Displays a background notification progress bar during bulk contact updates to keep you informed of the sync progress.

---

## 🛠️ Tech Stack & Architecture

* **Language**: Kotlin (JVM Target 17)
* **UI Framework**: Android Jetpack Compose with Material 3 Design
* **State Management**: Kotlin Coroutines, StateFlow, and Android Architecture Components (`ViewModel`, `ComponentActivity`)
* **Local Storage**: SharedPreferences (using `Gson` serialization for complex entities)
* **Core Integrations**:
  * `BroadcastReceiver` (`CallReceiver`) monitors phone state transitions.
  * `ContactsContract` (Contacts Provider) queries contact details and updates per-contact custom ringtone URIs.
  * `CallLog` content provider to query and process recent calls.

---

## 🔑 Permissions Required

To operate in the background and modify ringtones, the app requires:

1. **Contacts (`READ_CONTACTS`, `WRITE_CONTACTS`)**: Needed to read your contact list and write custom ringtones back to individual contact cards.
2. **Phone & Call Logs (`READ_PHONE_STATE`, `READ_CALL_LOG`)**: Required to monitor incoming calls and process call logs to update contact scores.
3. **Notifications (`POST_NOTIFICATIONS`)**: Required on Android 13 (API 33) and above to show the sync status progress notification during batch updates.

---

## 🚀 Getting Started

### Prerequisites

* Android SDK 26 (Android 8.0) or higher.
* Android Studio / Gradle 8+.

### Build from Source

To build the debug APK:

```bash
./gradlew assembleDebug
```

### Run and Deploy

If you have an Android device or emulator connected with `adb` enabled, you can run the provided PowerShell utility to build and deploy the correct architecture-specific APK:

```powershell
./debug.ps1 -BuildType debug
```

---

## 🧼 Code Quality & Style

This project enforces `ktlint` for code style rules. Lint checking and formatting are run on commit via pre-commit hooks.

* **Initialize Pre-commit Hooks**:

  ```bash
  pre-commit install
  ```

* **Run Linter Check**:

  ```bash
  ./gradlew ktlintCheck
  ```

* **Auto-Format Code**:

  ```bash
  ./gradlew ktlintFormat
  ```
