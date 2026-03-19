# Module circuit-codegen

A Kotlin Symbol Processing (KSP) plugin that automatically generates `Ui.Factory` and `Presenter.Factory` implementations for Circuit.

## Usage

Annotate your UI or Presenter with `@CircuitInject`:

```kotlin
@CircuitInject(HomeScreen::class, AppScope::class)
@Composable
fun Home(state: HomeScreen.State, modifier: Modifier = Modifier) {
  // UI implementation
}
```

The processor will generate the appropriate factory class that wires up your UI or Presenter to the Circuit runtime.

## Generated Code Examples

### UI Functions

**Input:**
```kotlin
@CircuitInject(HomeScreen::class, AppScope::class)
@Composable
fun Home(state: HomeScreen.State, modifier: Modifier = Modifier) {
  // ...
}
```

**Generated:**
```kotlin
@ContributesMultibinding(AppScope::class)
public class HomeFactory @Inject constructor() : Ui.Factory {
  override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
    HomeScreen -> ui<HomeScreen.State> { state, modifier -> Home(state = state, modifier = modifier) }
    else -> null
  }
}
```

### UI Functions with Screen Injection

**Input:**
```kotlin
@CircuitInject(FavoritesScreen::class, AppScope::class)
@Composable
fun Favorites(state: FavoritesScreen.State, screen: FavoritesScreen, modifier: Modifier = Modifier) {
  // Access screen.userId, etc.
}
```

**Generated:**
```kotlin
@ContributesMultibinding(AppScope::class)
public class FavoritesFactory @Inject constructor() : Ui.Factory {
  override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
    is FavoritesScreen -> ui<FavoritesScreen.State> { state, modifier -> Favorites(state = state, modifier = modifier, screen = screen) }
    else -> null
  }
}
```

### Presenter Functions

**Input:**
```kotlin
@CircuitInject(FavoritesScreen::class, AppScope::class)
@Composable
fun FavoritesPresenter(screen: FavoritesScreen, navigator: Navigator): FavoritesScreen.State {
  // Presenter logic
}
```

**Generated:**
```kotlin
@ContributesMultibinding(AppScope::class)
public class FavoritesPresenterFactory @Inject constructor() : Presenter.Factory {
  override fun create(
    screen: Screen,
    navigator: Navigator,
    context: CircuitContext,
  ): Presenter<*>? = when (screen) {
    is FavoritesScreen -> presenterOf { FavoritesPresenter(screen = screen, navigator = navigator) }
    else -> null
  }
}
```

### Presenter Classes with Injection

**Input:**
```kotlin
@CircuitInject(FavoritesScreen::class, AppScope::class)
class FavoritesPresenter @Inject constructor() : Presenter<FavoritesScreen.State> {
  @Composable
  override fun present(): FavoritesScreen.State {
    // ...
  }
}
```

**Generated:**
```kotlin
@ContributesMultibinding(AppScope::class)
public class FavoritesPresenterFactory @Inject constructor(
  private val provider: Provider<FavoritesPresenter>,
) : Presenter.Factory {
  override fun create(
    screen: Screen,
    navigator: Navigator,
    context: CircuitContext,
  ): Presenter<*>? = when (screen) {
    is FavoritesScreen -> provider.get()
    else -> null
  }
}
```

### Assisted Injection (Dagger and Metro)

For presenters/UIs that need runtime parameters like `Screen` or `Navigator`:

**Input:**
```kotlin
class FavoritesPresenter @AssistedInject constructor(
  @Assisted private val screen: FavoritesScreen,
  @Assisted private val navigator: Navigator,
) : Presenter<FavoritesScreen.State> {
  @CircuitInject(FavoritesScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(screen: FavoritesScreen, navigator: Navigator): FavoritesPresenter
  }

  @Composable
  override fun present(): FavoritesScreen.State {
    // ...
  }
}
```

**Generated:**
```kotlin
@ContributesMultibinding(AppScope::class)
public class FavoritesPresenterFactory @Inject constructor(
  private val factory: FavoritesPresenter.Factory,
) : Presenter.Factory {
  override fun create(
    screen: Screen,
    navigator: Navigator,
    context: CircuitContext,
  ): Presenter<*>? = when (screen) {
    is FavoritesScreen -> factory.create(screen = screen, navigator = navigator)
    else -> null
  }
}
```

## Codegen Modes

The processor supports multiple DI frameworks via the `circuit.codegen.mode` KSP option:

### Anvil (default)

Uses `@ContributesMultibinding` from Anvil with Dagger/Jakarta inject annotations.

### Hilt

```kotlin
// Generated factory
@OriginatingElement(topLevelClass = FavoritesPresenter::class)
public class FavoritesPresenterFactory @Inject constructor(
  private val factory: FavoritesPresenter.Factory,
) : Presenter.Factory { ... }

// Generated module
@Module
@InstallIn(SingletonComponent::class)
@OriginatingElement(topLevelClass = FavoritesPresenter::class)
public abstract class FavoritesPresenterFactoryModule {
  @Binds
  @IntoSet
  public abstract fun bindFavoritesPresenterFactory(
    favoritesPresenterFactory: FavoritesPresenterFactory
  ): Presenter.Factory
}
```

### kotlin-inject-anvil

**Input:**
```kotlin
@Inject
@CircuitInject(FavoritesScreen::class, AppScope::class)
class FavoritesPresenter : Presenter<FavoritesScreen.State> { ... }
```

**Generated:**
```kotlin
@Inject
@ContributesBinding(
  AppScope::class,
  multibinding = true,
)
@Origin(FavoritesPresenter::class)
public class FavoritesPresenterFactory(
  private val provider: () -> FavoritesPresenter,
) : Presenter.Factory { ... }
```

### Metro

**Input:**
```kotlin
@Inject
@CircuitInject(FavoritesScreen::class, AppScope::class)
class FavoritesPresenter : Presenter<FavoritesScreen.State> { ... }
```

**Generated:**
```kotlin
@Inject
@ContributesIntoSet(AppScope::class)
@Origin(FavoritesPresenter::class)
public class FavoritesPresenterFactory(
  private val provider: Provider<FavoritesPresenter>,
) : Presenter.Factory { ... }
```

## Configuration

Configure via KSP options in your `build.gradle.kts`:

```kotlin
ksp {
  arg("circuit.codegen.mode", "ANVIL") // ANVIL, HILT, KOTLIN_INJECT_ANVIL, or METRO
  arg("circuit.codegen.lenient", "true") // Relaxed annotation matching
}
```

| Option                    | Description                 | Default |
|---------------------------|-----------------------------|---------|
| `circuit.codegen.mode`    | DI framework mode           | `ANVIL` |
| `circuit.codegen.lenient` | Relaxed annotation matching | `false` |
