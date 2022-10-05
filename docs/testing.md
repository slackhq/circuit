Testing
=======

Circuit is designed to make testing as easy as possible. Its core components are not mockable nor do they need to be mocked. Fakes are provided where needed, everything else can be used directly.

Circuit will have a test artifact containing APIs to aid testing both presenters and composable UIs:

1. `Presenter.test()` - an extension function that bridges the Compose and coroutines world. Use of this function is recommended for testing presenter state emissions and incoming UI events. Under the hood it leverages [Molecule](https://github.com/cashapp/molecule) and [Turbine](https://github.com/cashapp/turbine).
2. `FakeNavigator` - a test fake implementing the Circuit/Navigator interface. Use of this object is recommended when testing screen navigation (ie. goTo, pop/back).

## Example

Testing a Circuit Presenter and UI is a breeze! Consider the following example:

```kotlin
data class Favorite(id: Long, ...)

@Parcelable
object FavoritesScreen : Screen {
  sealed interface State : CircuitUiState {
    object Loading : State
    object NoFavorites : State
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
  val presenter = PetListPresenter(navigator, repo)
  
  presenter.test {
    assertThat(awaitItem()).isEqualTo(PetListScreen.State.Loading)
    val resultsItem = awaitItem() as Results
    assertThat(resultsItem.favorites).isEqualTo(favorites)
  }
}

The same helper can be used when testing how the presenter responds to incoming events: 

@Test 
fun `present - navigate to favorite screen`() = runTest {
  val repo = TestFavoritesRepository(Favorite(123L))
  val presenter = PetListPresenter(navigator, repo)
  
  presenter.test {
    assertThat(awaitItem()).isEqualTo(PetListScreen.State.Loading)
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

UI tests can be driven directly through ComposeTestRule and use its Espresso-esque API for assertions:

```kotlin
@Test
fun favoritesList_show_favorites_for_result_state() = runTest {
  val favorites = listOf(Favorite(1L, ...)

  composeTestRule.run {
    setContent { 
      // bootstrap the UI in the desired state
      FavoritesList(
        state = FavoriteScreen.State.Results(favorites) { /* event callback */ }
      )
    }

    onNodeWithTag("no favorites").assertDoesNotExist()
    onNodeWithText("Your Favorites").assertIsDisplayed()
    onAllNodesWithTag("favorite").assertCountEquals(1)
  }
}
```


### Future: Android UI Unit Tests via Paparazzi

We’ve started exploring use of [Paparazzi](https://github.com/cashapp/paparazzi), which allows us to render Android UI without a physical device or emulator. More to come soon, but in short it would work similar to the above but be for purely non-functional 1:1 state ↔ UI tests.

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
