Counter – KMM Apps
==================

This projects contains KMM apps for the multiplatform Counter sample.

There are four apps:
- Android (under `src/androidMain`)
- Desktop (under `src/jvmMain`)
- iOS (under `iosApp`)
- WASM (under `src/wasmJsMain`)

The goal of these samples is to share presentation logic but _not_ UI. Android/WASM/Desktop use Compose UI, iOS uses SwiftUI.

### Running the apps

To run the Android app, open the project in Android Studio and run the app from there.

To run the Desktop app, run the `main` function in `DesktopCounterCircuit` in your IDE.

To run the iOS app, run the Counter iOS target in IntelliJ/Studio or open the Counter Xcode project and run the app from there.

To run the WASM/JS app, run `./gradlew :samples:counter:apps:wasmJsBrowserDevelopmentRun --continuous` and it'll open automatically in your default browser.
