# Contributors Guide

Note that this project is considered READ-ONLY. You are welcome to discuss or ask questions in the
discussions section of the repo, but we do not normally accept external contributions without prior
discussion.

## Development

Check out this repo with Android Studio. It's a standard gradle project and conventional to
checkout.

Circuit is a Kotlin Multiplatform project, so ensure you have your environment set up 
accordingly: https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-setup.html

The primary project is `circuit`. The primary sample is `samples/star`.

This project is written in Kotlin and should only use Kotlin.

Code formatting is checked via [Spotless](https://github.com/diffplug/spotless). To run the
formatter, use the `spotlessApply` command.

```bash
./gradlew spotlessApply
```

### iOS

To build any of the iOS checks, you must do the following:
1. Run `bundle install` to set up fastlane.
2. Have `swiftformat` installed. You can install it via `brew install swiftformat`.