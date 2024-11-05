// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.imageviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.ScaleToBounds
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import coil.request.ImageRequest.Builder
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.RecordContentProvider
import com.slack.circuit.foundation.thenIf
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Overlay
import com.slack.circuit.star.common.BackPressNavIcon
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.imageviewer.FlickToDismissState.FlickGestureState.Dismissed
import com.slack.circuit.star.imageviewer.ImageViewerScreen.Event.Close
import com.slack.circuit.star.imageviewer.ImageViewerScreen.Event.NoOp
import com.slack.circuit.star.imageviewer.ImageViewerScreen.State
import com.slack.circuit.star.transition.PetImageBoundsKey
import com.slack.circuit.star.transition.PetImageElementKey
import com.slack.circuit.star.ui.StarTheme
import com.slack.circuit.star.ui.rememberSystemUiController
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.ImmutableList
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

class ImageViewerPresenter
@AssistedInject
constructor(
  @Assisted private val screen: ImageViewerScreen,
  @Assisted private val navigator: Navigator,
) : Presenter<State> {
  @CircuitInject(ImageViewerScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(screen: ImageViewerScreen, navigator: Navigator): ImageViewerPresenter
  }

  @Composable
  override fun present(): State {
    return State(id = screen.id, url = screen.url, placeholderKey = screen.placeholderKey) { event
      ->
      when (event) {
        Close -> navigator.pop()
        NoOp -> {}
      }
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@CircuitInject(ImageViewerScreen::class, AppScope::class)
@Composable
fun ImageViewer(state: State, modifier: Modifier = Modifier) = SharedElementTransitionScope {
  var showChrome by remember { mutableStateOf(true) }
  val systemUiController = rememberSystemUiController()
  systemUiController.isSystemBarsVisible = showChrome
  DisposableEffect(systemUiController) {
    systemUiController.statusBarDarkContentEnabled = false
    val originalSystemBarsBehavior = systemUiController.systemBarsBehavior
    // Set BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE so the UI doesn't jump when it hides
    systemUiController.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    onDispose {
      // TODO this is too late for some reason
      systemUiController.isSystemBarsVisible = true
      systemUiController.systemBarsBehavior = originalSystemBarsBehavior
    }
  }

  val overlayTransition = getAnimatedScope(Overlay)?.transition
  val backgroundColor =
    overlayTransition
      ?.animateColor(transitionSpec = { tween() }, label = "Background color") { state ->
        when (state) {
          EnterExitState.PreEnter -> Color.Transparent
          EnterExitState.Visible -> Color.Black
          EnterExitState.PostExit -> Color.Transparent
        }
      }
      ?.value ?: Color.Black

  StarTheme(useDarkTheme = true) {
    Surface(
      modifier = modifier.fillMaxSize().animateContentSize(),
      color = backgroundColor,
      contentColor = Color.White,
    ) {
      Box(Modifier.fillMaxSize()) {
        // Image + scrim

        val dismissState = rememberFlickToDismissState()
        if (dismissState.gestureState is Dismissed) {
          state.eventSink(Close)
        }
        // TODO bind scrim with flick. animate scrim out after flick finishes? Or with flick?
        FlickToDismiss(
          state = dismissState,
          modifier =
            Modifier.thenIf(!dismissState.willDismissOnRelease) {
              sharedBounds(
                sharedContentState = rememberSharedContentState(key = PetImageBoundsKey(state.id)),
                animatedVisibilityScope = requireAnimatedScope(Overlay),
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(easing = LinearEasing)),
                resizeMode = ScaleToBounds(ContentScale.Crop, Center),
              )
            },
        ) {
          val zoomableState = rememberZoomableState(ZoomSpec(maxZoomFactor = 4f))
          val imageState = rememberZoomableImageState(zoomableState)
          // TODO loading loading indicator if there's no memory cached placeholderKey
          ZoomableAsyncImage(
            model =
              Builder(LocalContext.current)
                .data(state.url)
                .apply {
                  state.placeholderKey?.let {
                    placeholderMemoryCacheKey(it)
                    crossfade(AnimationConstants.DefaultDurationMillis)
                  }
                }
                .build(),
            contentDescription = "TODO",
            modifier =
              Modifier.fillMaxSize().thenIf(!dismissState.willDismissOnRelease) {
                sharedElement(
                  state = rememberSharedContentState(key = PetImageElementKey(state.url)),
                  animatedVisibilityScope = requireAnimatedScope(Overlay),
                )
              },
            state = imageState,
            onClick = { showChrome = !showChrome },
          )
        }

        // TODO pick color based on if image is underneath it or not. Similar to badges
        val backVisible =
          showChrome &&
            overlayTransition?.targetState?.let { it == EnterExitState.Visible } != false

        AnimatedVisibility(backVisible, enter = fadeIn(), exit = fadeOut()) {
          CenterAlignedTopAppBar(
            title = {},
            navigationIcon = { BackPressNavIcon() },
            colors =
              TopAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                navigationIconContentColor = Color.Transparent,
                titleContentColor = Color.Transparent,
                actionIconContentColor = Color.Transparent,
              ),
          )
        }
      }
    }
  }
}

// TODO
//  generalize this when there's a factory pattern for it in Circuit
//  shared element transitions?
class ImageViewerAwareNavDecoration(
  private val defaultNavDecoration: NavDecoration = NavigatorDefaults.DefaultDecoration
) : NavDecoration {
  @Suppress("UnstableCollections")
  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: ImmutableList<T>,
    backStackDepth: Int,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    val firstArg = args.firstOrNull()
    val decoration =
      if (firstArg is RecordContentProvider<*> && firstArg.record.screen is ImageViewerScreen) {
        NavigatorDefaults.EmptyDecoration
      } else {
        defaultNavDecoration
      }
    decoration.DecoratedContent(args, backStackDepth, modifier, content)
  }
}
