# Panda AI by Max

A production-ready Android voice assistant that combines speech recognition, LLM-powered conversations, and smart device controls. Built with Kotlin, Material 3, and Room for persistent chat history.

## Getting Started

1. **Open in Android Studio**
   - `File` → `Open` → choose the `PandaAIByMax` directory.
   - Allow Android Studio to sync Gradle and download dependencies.

2. **Configure the AI API key**
   - Open `app/src/main/java/com/max/pandaai/ai/AIService.kt`.
   - Replace `YOUR_OPENAI_API_KEY` with a valid key (or adjust the endpoint to your preferred provider).
   - For Google Play uploads, store secrets securely using Play Console features, remote config, or an encrypted keystore.

3. **Run the app**
   - Use a physical device or emulator with Google Play services.
   - Grant microphone, phone, and SMS permissions when prompted so Panda AI can complete actions on your behalf.

4. **Build release artifacts**
   - From Android Studio: `Build` → `Generate Signed Bundle / APK`.
   - Or via command line: `./gradlew assembleRelease`.

## Key Features

- Hands-free conversations powered by large language models.
- Intent routing for app launching, calls, SMS, calendar events, alarms, and camera access.
- Text-to-speech responses with customizable voice pitch and speed.
- Persistent chat history with Room and quick wipe via menu.
- Material 3 UI with dark mode, glowing listening animation, and a Panda-branded splash screen.
- Settings screen to rename the assistant, manage preferences, and review privacy policy.
- Explicit runtime permission explanations for microphone, phone, and SMS access.

## Privacy & Compliance

- Voice capture happens only after tapping the microphone.
- Sensitive actions (calls, SMS) ask for permission and final confirmation.
- A privacy policy screen is bundled at `Settings → Privacy Policy` for Play Store review.
- The app stores chat history locally; clear it anytime from the overflow menu.

## Play Store Checklist

- Update the Privacy Policy URL in the Play Console to match the in-app statement.
- Provide screenshots showcasing light and dark themes.
- Double-check that all sensitive permissions are declared and justified in the listing.
- Run the pre-launch report to verify microphone, TTS, and speech recognition work on target devices.

Happy building with Panda AI by Max! 🐼
