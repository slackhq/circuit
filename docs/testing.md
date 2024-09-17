Testing
=======

Circuit is designed to make testing as easy as possible. Its core components are not mockable nor do they need to be mocked. Fakes are provided where needed, everything else can be used directly.

Circuit offers a test artifact containing APIs to aid testing both presenters and composable UIs:

- `presenterTestOf()` - a top-level function that wraps a composable function to bridge the Compose and coroutines world. Use of this function is recommended for testing presenter state emissions and incoming UI events. Under the hood it leverages [Molecule](https://github.com/cashapp/molecule) and [Turbine](https://github.com/cashapp/turbine). It returns a `CircuitReceiveTurbine`, a custom implementation of `ReceiveTurbine` that modifies the behavior of `awaitItem()` to only emit _changed_ items (i.e. "distinct until changed").
- `Presenter.test()` - an extension function on `Presenter` that bridges to `presenterTestOf()`.
- `FakeNavigator` - a test fake implementing the `Navigator` interface. Use of this object is recommended when testing screen navigation (ie. goTo, pop/back). This acts as a real navigator and exposes recorded information for testing purposes.
- `TestEventSink` - a generic test fake for recording and asserting event emissions through an event sink function.

## Installation

Test helpers are available via the `circuit-test` artifact.

```kotlin
testImplementation("com.slack.circuit:circuit-test:<version>")
```

For Gradle JVM projects, you can use Gradle test fixtures syntax on the core circuit artifact.

```kotlin
testImplementation(testFixtures("com.slack.circuit:circuit:<version>"))
```

## Example

Testing a Circuit Presenter and UI is a breeze! Consider the following example:

```kotlin
data class Favorite(id: Long, ...)

@Parcelable
data object FavoritesScreen : Screen {
  sealed interface State : CircuitUiState {
    data object Loading : State
    data object NoFavorites : State
    data class Results(
      val list: List<Favorite>,
      val eventSink: (Event) -> Unit
    ) : State
  }
  
  sealed interface Event : CircuitUiEvent {
    data class ClickFavorite(id: Long): Event
  }
}

class FavoritesPresenter @Inject constructor(
    navigator: Navigator,
    repo: FavoritesRepository
) : Presenter<State> {
  @Composable override fun present(): State {
    val favorites by produceState<List<Favorites>?>(null) {
      value = repo.getFavorites()
    }
    
    return when {
      favorites == null -> Loading
      favorites.isEmpty() -> NoFavorites
      else ->
        Results(favorites) { event ->
          when (event) {
            is ClickFavorite -> navigator.goTo(FavoriteScreen(event.id))
          }
        }
    }
  }
}

@Composable
fun FavoritesList(state: FavoritesScreen.State) {
  when (state) {
    Loading -> Text(text = stringResource(R.string.loading_favorites))
    NoFavorites -> Text(
      modifier = Modifier.testTag("no favorites"),
      text = stringResource(R.string.no_favorites)
    )
    is Results -> {
      Text(text = "Your Favorites")
      LazyColumn {
        items(state.list) { Favorite(it, state.eventSink) }
      }
    }
  }
}

@Composable
private fun Favorite(favorite: Favorite, eventSink: (FavoritesScreen.Event) -> Unit) {
  Row(
    modifier = Modifier.testTag("favorite"),
    onClick = { eventSink(ClickFavorite(favorite.id)) }
  ) {
    Image(
      drawable = favorite.drawable, 
      contentDescription = stringResource(R.string.favorite_image_desc)
    )
    Text(text = favorite.name)
    Text(text = favorite.date)
  }
}
```

### Presenter Unit Tests

Here’s a test to verify presenter emissions using the `Presenter.test()` helper. This function acts as a shorthand over Molecule + Turbine to give you a `ReceiveTurbine.() -> Unit` lambda.

```kotlin
@Test 
fun `present - emit loading state then list of favorites`() = runTest {
  val favorites = listOf(Favorite(1L, ...))

  val repo = TestFavoritesRepository(favorites)
  val presenter = FavoritesPresenter(navigator, repo)
  
  presenter.test {
    assertThat(awaitItem()).isEqualTo(FavoritesScreen.State.Loading)
    val resultsItem = awaitItem() as Results
    assertThat(resultsItem.favorites).isEqualTo(favorites)
  }
}
```

The same helper can be used when testing how the presenter responds to incoming events: 

```kotlin
@Test 
fun `present - navigate to favorite screen`() = runTest {
  val repo = TestFavoritesRepository(Favorite(123L))
  val presenter = FavoritesPresenter(navigator, repo)
  
  presenter.test {
    assertThat(awaitItem()).isEqualTo(FavoritesScreen.State.Loading)
    val resultsItem = awaitItem() as Results
    assertThat(resultsItem.favorites).isEqualTo(favorites)
    val clickFavorite = FavoriteScreen.Event.ClickFavorite(123L)
    
    // simulate user tapping favorite in UI
    resultsItem.eventSink(clickFavorite)
    
    assertThat(navigator.awaitNextScreen()).isEqualTo(FavoriteScreen(clickFavorite.id))
  }
}
```

### Android UI Instrumentation Tests

UI tests can be driven directly through `ComposeTestRule` and use its Espresso-esque API for assertions:

Here is also a good place to use a `TestEventSink` and assert expected event emissions from specific UI interactions.

```kotlin
@Test
fun favoritesList_show_favorites_for_result_state() = runTest {
  val favorites = listOf(Favorite(1L, ...))
  val events = TestEventSink<FavoriteScreen.Event>()

  composeTestRule.run {
    setContent { 
      // bootstrap the UI in the desired state
      FavoritesList(
        state = FavoriteScreen.State.Results(favorites, events)
      )
    }

    onNodeWithTag("no favorites").assertDoesNotExist()
    onNodeWithText("Your Favorites").assertIsDisplayed()
    onAllNodesWithTag("favorite").assertCountEquals(1)
      .get(1)
      .performClick()
    
    events.assertEvent(FavoriteScreen.Event.ClickFavorite(1L))
  }
}
```


### Snapshot Tests

Because Circuit UIs simply take an input state parameter, snapshot tests via [Paparazzi](https://github.com/cashapp/paparazzi) or [Roborazzi](https://github.com/takahirom/roborazzi) are a breeze.

This allows allows you to render UI without a physical device or emulator and assert pixel-perfection on the result.

```kotlin
@Test
fun previewFavorite() {
  paparazzi.snapshot { PreviewFavorite() }
}
```

These are easy to maintain and review in GitHub.

Another neat idea is we think this will make it easy to stand up compose preview functions for IDE use and reuse them.

```kotlin
// In your main source
@Preview
@Composable
internal fun PreviewFavorite() {
  Favorite()
}

// In your unit test
@Test
fun previewFavorite() {
  paparazzi.snapshot { PreviewFavorite() }
}
```
