Counter – KMM Apps
==================

This projects contains KMM apps for the multiplatform Counter sample.

There are four apps:
- Android (under `src/androidMain`)
- Desktop (under `src/jvmMain`)
- Desktop (under `src/jsMain`)
- iOS (under `iosApp`)

The goal of these samples is to share presentation logic but _not_ UI. Android/JS/Desktop use Compose UI, iOS uses SwiftUI.

### Running the apps

To run the Android app, open the project in Android Studio and run the app from there.

To run the Desktop app, run the `main` function in `DesktopCounterCircuit` in your IDE.

To run the iOS app, run the Counter iOS target in IntelliJ/Studio or open the Counter Xcode project and run the app from there.
