package com.slack.circuit

/** TODO */
fun interface ScreenViewFactory {
  fun createView(screen: Screen, container: ContentContainer): ScreenView?
}

data class ScreenView(
  val container: ContentContainer,
  val ui: Ui<*, *>,
// TODO does this kind of thing eventually move to compose Modifier instead?
//  val uiMetadata: UiMetadata = UiMetadata()
)

// Example
// class FavoritesViewFactory @Inject constructor(
//  private val picasso: Picasso,
// ) : ViewFactory {
//  override fun createView(
//    screen: Screen,
//    context: Context,
//    parent: ViewGroup
//  ): ScreenView? {
//    val view = when (screen) {
//      is AddFavorites -> {
//        AddFavoritesView(
//          context = context,
//          picasso = picasso,
//        )
//      }
//      else -> return null
//    }
//
//    return ScreenView(
//      view = view,
//      ui = view as Ui<*, *>,
//      uiMetadata = UiMetadata(treatment = FULL_SCREEN, hideTabs = true)
//    )
//  }
// }
