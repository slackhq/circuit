Counter – KMM Apps
==================

This projects contains KMM apps for the multiplatform Counter sample.

There are three apps:
- Android (under `src/androidMain`)
- Desktop (under `src/jvmMain`)
- iOS (under `Counter`)

The goal of these samples is to share presentation logic but _not_ UI. Android and Desktop use Compose UI, iOS uses SwiftUI.

### Running the apps

To run the Android app, open the project in Android Studio and run the app from there.

To run the Desktop app, run the `main` function in `DesktopCounterCircuit` in your IDE.

To run the iOS app, run the Counter iOS target in IntelliJ/Studio or open the Counter Xcode project and run the app from there. **Note** to run the iOS project, you need to run `./gradlew :samples:counter:linkDebugFrameworkIosSimulatorArm64` followed by `pod install` from the `samples/counter/apps` dir first. This ensures that the Xcode project sees the produced framework correctly.
