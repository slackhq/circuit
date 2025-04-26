CircuitX
========

CircuitX is a suite of extension artifacts for Circuit. These artifacts are intended to be
batteries-included implementations of common use cases, such as out-of-the-box `Overlay` types or
Android navigation interop.

These packages differ from Circuit's core artifacts in a few ways:

- Their APIs may change more frequently during Circuit's development.
- These artifacts won't ship with their own baseline profiles.
- These artifacts are under the `com.slack.circuitx` package prefix.
- These artifacts may be platform-specific where appropriate.

| Artifact                                         | Documentation                                        |
|--------------------------------------------------|------------------------------------------------------|
| `com.slack.circuitx:circuitx-android`            | [Android](circuitx/android.md)                       |
| `com.slack.circuitx:circuitx-effects`            | [Effects](circuitx/effects.md)                       |
| `com.slack.circuitx:circuitx-gesture-navigation` | [Gesture Navigation](circuitx/gesture-navigation.md) |
| `com.slack.circuitx:circuitx-overlays`           | [Overlays](circuitx/overlays.md)                     |