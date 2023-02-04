Setting up Circuit
==================

Setting up Circuit is a breeze! Just add the following to your build:

## Installation

```kotlin
dependencies {
  implementation("com.slack.circuit:circuit-core:<version>")
}
```

## Setup

### Android

Circuit requires two headless `ViewModel`s to be available in your application: `BackStackRecordLocalProviderViewModel` and `Continuity`. These would usually be wired up in a DI framework like Dagger, but a simple `ViewModelProvider.Factory` can be used as well.

```kotlin
val circuitViewModelProviderFactory = object : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return when (modelClass) {
      BackStackRecordLocalProviderViewModel::class.java -> BackStackRecordLocalProviderViewModel()
      Continuity::class.java -> Continuity()
      else -> ...
    } as T
  }
}
```

### JVM

No extra configuration needed!

## Platform Support

Circuit is a multiplatform library, but not all features are available on all platforms. The following table shows which features are available on which platforms:

- ✅ Available
- ❌ Not available
- – Not applicable

| Feature                   | Android | JVM | Notes |
|---------------------------| ------- |--| |
| `Backstack`               | ✅ | ✅ | |
| `CircuitContent`          | ✅ | ✅ | |
| `ContentWithOverlays` | ✅ | ✅ | |
| `NavigableCircuitContent` | ✅ | ✅ | |
| `Navigator`               | ✅ | ✅ | |
| `SaveableBackstack`       | ✅ | ✅ | Saveable is a no-op on desktop. |
| `rememberCircuitNavigator` | ✅ | ✅ | |
| `rememberRetained` | ✅ | ✅ | |
