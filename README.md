# FamChat

FamChat is an Android-first private messenger.

## Current setup
- Kotlin + Jetpack Compose
- Package: `com.famchat.app`
- Minimum Android: 8.0 (API 26)
- Dark navy interface with blue accents

## Current milestone
This branch contains the initial Android project scaffold and a static chat-list UI preview. Chat data is sample data only; authentication, user search, messaging, and Firebase are not connected yet.

## Open in Android Studio
1. Clone this repository.
2. Open the project root (the folder containing `settings.gradle.kts`).
3. Use JDK 17 and allow Gradle sync to finish.
4. Run the `app` configuration on an Android 8.0+ device or emulator.

## Firebase setup (next milestone)
Create an Android app in Firebase Console using package name `com.famchat.app`, download `google-services.json`, and place it in the `app/` directory. Firebase Authentication, Cloud Firestore, and push notifications will be integrated in a later step.

## Branch
`setup/android-mvp`
