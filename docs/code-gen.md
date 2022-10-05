Code Generation
===============

We plan to include code gen tools to cover most of any boilerplate, making the common paths simple while allowing for more complex hand-written structures when needed.

At a high level using the examples above, we want to generate the following bits of code for users

```kotlin
class FavoritesScreenUiFactory
class FavoritesScreenPresenterFactory
private fun favoritesUi()
```

We’re intentionally saving implementing this step last as it makes making API changes more difficult. Follow along progress here: https://github.com/slackhq/circuit/issues/13
