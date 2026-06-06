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
* **Customizable Call Score Additions**: Configure custom score addition points for missed and rejected calls independently in Settings.
* **Backup & Restore**: Safely export your settings, contact scores, and ringtone mappings to a file, and import them back at any time.
* **Service Pauser**: Temporarily pause custom ringtone changes and automatically restore all contacts to their default/original settings. Extremely useful before uninstalling the app to ensure your device's contacts are left in their original state.
* **Fallback Default Ringtone**: Choose a fallback ringtone to use when restoring contact ringtones if their original custom ringtone is deleted, missing, or was set to system default.
* **Robust Diagnostics**: Turn on logging to capture errors and background events locally. Review logs in the built-in system log viewer, clear logs, or copy them to the clipboard to report bugs.
* **Progressive Sync Notifications**: Displays a background notification progress bar during bulk contact updates to keep you informed of the sync progress.

---

## 📖 User Guide: How to Use the App

Smart Ringtone is designed to run automatically in the background, but there are a few simple setup and configuration steps to get the most out of it.

### 1. First-Time Setup & Permissions

When you first open the app, you will be prompted to grant the necessary permissions. These are essential for the app to function:

* **Contacts (`READ_CONTACTS`, `WRITE_CONTACTS`)**: Allows the app to read your contact list and update custom ringtones directly on each contact's card.
* **Phone & Call Logs (`READ_PHONE_STATE`, `READ_CALL_LOG`)**: Allows the app to detect incoming calls, read call status (missed/rejected/answered), and adjust contact scores accordingly.
* **Storage / Media Audio (`READ_EXTERNAL_STORAGE`, `READ_MEDIA_AUDIO`)**: Allows the app to access, select, and read custom audio files from your device's internal storage to use as custom contact ringtones.
* **Notifications (`POST_NOTIFICATIONS`)**: Used to display background sync status progress notifications during bulk contact updates.

### 2. Configure Your Ringtone Sequence

Head over to the **Ringtones** tab. This is where you configure the sequence of ringtones used for escalation:

* **Add Ringtone**: Tap the **Add** button. You can choose to add a standard audio file from your device, or add a **Blank Ringtone** (which creates silence/no sound, useful if you want to silence early missed calls from contacts).
* **Preview Ringtones**: Tap the play/pause icon next to any ringtone to preview it.
* **Reorder**: Use the **Move Up** / **Move Down** arrows to change the order.
  * *Call Score = 0*: The contact uses their original custom ringtone or system default.
  * *Call Score = 1*: Plays the first ringtone in the list.
  * *Call Score = 2*: Plays the second ringtone in the list, and so on.
  * *Call Score >= N*: Caps at the last (usually loudest/most urgent) ringtone in the list.
* **Unavailable Files**: If an audio file in your sequence is deleted or moved, the app will flag it. Tap **Pick File** on the prompt to replace it.

### 3. Track and Manage

The **Contacts** tab lists all the contacts on your device:

* **Search**: Use the search bar to find contacts by name. The search supports multi-term space-separated queries (e.g., searching "john smith" matches any contacts containing both terms).
* **Sort**: Sort your contacts by **Name** or **Score**.
* **Manually Reset Score**: Tap on a contact and select **Reset** to reset their score back to `0`.
* **Reset All**: Tap the reset icon in the top bar to reset the call scores of all contacts back to `0`.

### 4. Monitor Call Logs & Rebuild Score History

You can keep track of how contact scores change in real time:

* **Call Log Tab**: Turn on **Enable Log Tab** in settings to display the log tab in the bottom bar. Here, you'll see a detailed log of all call events, timestamps, and score modifications.
* **Rescan History**: If you just installed the app and want to build contact scores using your existing call history, tap **Rescan History** (the sync icon) in the Call Log or Contacts tab.
  > [!IMPORTANT]
  > Rescanning will reset all scores to `0` and recalculate everything from the beginning of your system call log.

### 5. Customize Settings

Navigate to the **Settings** tab to fine-tune the app:

* **Appearance**: Toggle between **Light Theme**, **Dark Theme**, or **System Default**.
* **Fallback Default Ringtone**: Set a default backup ringtone to use if a contact's original custom ringtone was deleted, missing, or was set to system default.
* **Call Score Additions**: Customize how many points are added to a contact's score. You can independently increase or decrease the score additions for **Missed calls** (default: +1) and **Rejected calls** (default: +2).
* **Backup & Restore**: Safely export your settings, contact scores, and ringtone mappings to a backup file, and import them back at any time.
* **Diagnostics**: Enable local error logging, view system diagnostic logs, or copy them to report bugs.

### 6. Pausing & Uninstalling the App

> [!WARNING]
> Before uninstalling Smart Ringtone, you should pause the customization service to clean up your contact ringtones.

* Go to **Settings** -> **Pause Service**.
* When enabled, the app will temporarily restore all contact ringtones to their original settings/system defaults.
* Keep this setting enabled before uninstalling the app to ensure your device's contacts are successfully reverted back to their original states.

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
3. **Storage / Media Audio (`READ_EXTERNAL_STORAGE` for Android 12 & below, `READ_MEDIA_AUDIO` for Android 13+)**: Required to let users browse, select, and read custom ringtones from the device's local storage.
4. **Notifications (`POST_NOTIFICATIONS`)**: Required on Android 13 (API 33) and above to show the sync status progress notification during batch updates.

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
