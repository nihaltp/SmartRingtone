# Contributing to Smart Ringtone

Thank you for your interest in contributing to Smart Ringtone! Contributions from the community help make this utility better for everyone.

---

## 🏗️ Getting Started

### 1. Prerequisites

Before you start, make sure you have:

* **Android Studio** (Koala or newer recommended)
* **Android SDK Platform 34** (Target SDK)
* **JDK 17** configured in your development environment
* **pre-commit** installed on your machine (`pip install pre-commit` or via your package manager)

### 2. Setting Up the Project

1. Clone the repository:

    ```bash
    git clone https://github.com/nihaltp/SmartRingtone.git
    cd SmartRingtone
    ```

2. Install git pre-commit hooks to ensure automated code formatting checks run before every commit:

    ```bash
    pre-commit install
    ```

---

## 🧼 Code Style & Quality Guidelines

To keep the codebase clean and maintainable, we enforce the following guidelines:

* **Language**: Standard Kotlin patterns, utilizing asynchronous flows (Coroutines, StateFlow) where appropriate.
* **UI**: Declarative layouts using Jetpack Compose and Material 3 design tokens. Predefined UI tokens in `ui/MainScreen.kt` (such as `BackgroundColor`, `AccentColor`, etc.) should be favored over inline styling.
* **Linting & Formatting**: We use `ktlint` to enforce style guide conformity.

  * To check code quality:

    ```bash
    ./gradlew ktlintCheck
    ```

  * To automatically fix formatting violations:

    ```bash
    ./gradlew ktlintFormat
    ```

  * Make sure `ktlintCheck` passes locally before committing.

---

## 🧪 Testing

Before submitting a Pull Request, verify that the project builds and all tests pass.

* **Unit Tests**:

    ```bash
    ./gradlew test
    ```

* **Instrumented Tests** (requires a connected device or emulator):

    ```bash
    ./gradlew connectedAndroidTest
    ```

---

## 🚀 Building & Running

* To build a debug APK:

    ```bash
    ./gradlew assembleDebug
    ```

* To build and automatically deploy the matching CPU ABI version of the app to your connected device/emulator:

    ```powershell
    # Windows PowerShell
    ./debug.ps1 -BuildType debug
    ```

---

## 📥 Submitting Changes

1. **Branching**: Create a descriptive branch name from `main` (e.g., `feature/contact-search-improvement` or `bugfix/launcher-icon-rendering`).
2. **Commit Messages**: Keep commit messages concise and descriptive (e.g., `feat: add swipe-to-delete custom ringtone` or `fix: handle null phone number lookup`).
3. **Formatting**: Ensure you run `./gradlew ktlintFormat` to fix any stylistic discrepancies.
4. **Pull Request**: Open a pull request against the `main` branch. Provide a brief summary of the changes made, why they are necessary, and steps on how to test them.
