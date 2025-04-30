CircuitX provides `NavDecoration` implementation which support navigation through appropriate
gestures on certain platforms.

```kotlin
dependencies {
  implementation("com.slack.circuit:circuitx-gesture-navigation:<version>")
}
```

To enable gesture navigation support, you can use the use the `GestureNavigationDecoration`function:

```kotlin
NavigableCircuitContent(
  navigator = navigator,
  backStack = backstack,
  decoration = GestureNavigationDecoration(
    // Pop the back stack once the user has gone 'back'
    navigator::pop
  )
)
```

### Android

On Android, this supports the [Predictive back gesture](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture) which is available on Android 14 and later (API level 34+). On older platforms, Circuit's default
`NavDecoration` decoration is used instead.

<figure>
  <video controls width="300" loop=true>
    <source src="../../videos/gesturenav_android.mp4" type="video/mp4" />
  </video>
  <figcaption><a href="https://github.com/slackhq/circuit/tree/main/samples/star">Star sample</a> running on an Android 14 device</figcaption>
</figure>

### iOS

On iOS, this simulates iOS's 'Interactive Pop Gesture' in Compose UI, allowing the user to swipe Circuit UIs away. As this is
a simulation of the native behavior, it does not match the native functionality perfectly. However, it is a good approximation.

<figure>
  <video controls width="300" loop=true>
    <source src="../../videos/gesturenav_ios.mp4" type="video/mp4" />
  </video>
  <figcaption><a href="https://github.com/chrisbanes/tivi">Tivi</a> app running on iPhone</figcaption>
</figure>

### Other platforms

On other platforms we defer to Circuit's default `NavDecoration` decoration.
