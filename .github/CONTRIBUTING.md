# Contributors Guide

## Development

Check out this repo with Android Studio. It's a standard gradle project and conventional to
checkout.

Circuit is a Kotlin Multiplatform project, so ensure you have your environment set up 
accordingly: https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-setup.html

This project is written in Kotlin and should only use Kotlin.

Code formatting is handled by [Kempt](https://github.com/ZacSweers/kempt). Install it with
Homebrew, then install its pre-commit hook:

```bash
brew install ZacSweers/tap/kempt-fmt
kempt install-hook
```

The hook runs Kempt on staged files before each commit. To format or check the whole project
manually, use:

```bash
kempt format
kempt check
```

You can build and run the CI checks (aside from instrumentation tests) with:

```bash
./gradlew circuitCi
```

### iOS

To build any of the iOS checks, you must do the following:

1. Run `bundle install` to set up fastlane.
2. Have `swiftformat` installed. You can install it via `brew install swiftformat`.
