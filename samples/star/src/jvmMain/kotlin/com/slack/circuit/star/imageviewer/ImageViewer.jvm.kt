// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.imageviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.EaseInQuint
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest.Builder
import coil3.request.crossfade
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Overlay
import com.slack.circuit.star.common.BackPressNavIcon
import com.slack.circuit.star.imageviewer.ImageViewerScreen.Event.Close
import com.slack.circuit.star.imageviewer.ImageViewerScreen.State
import com.slack.circuit.star.transition.PetImageBoundsKey
import com.slack.circuit.star.transition.PetImageElementKey
import com.slack.circuit.star.ui.StarTheme
import dev.zacsweers.metro.AppScope

@CircuitInject(ImageViewerScreen::class, AppScope::class)
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
actual fun ImageViewer(state: State, modifier: Modifier) = SharedElementTransitionScope {
  var showChrome by remember { mutableStateOf(true) }

  val overlayTransition = findAnimatedScope(Overlay)?.transition
  val backgroundColor =
    overlayTransition
      ?.animateColor(
        label = "Background color",
        transitionSpec = {
          when (targetState) {
            EnterExitState.PreEnter,
            EnterExitState.Visible -> tween(delayMillis = 60, easing = EaseInQuint)
            EnterExitState.PostExit -> tween(easing = EaseOutQuint)
          }
        },
      ) { enterExitState ->
        when (enterExitState) {
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
        // Image (no zoom/flick on JVM)
        Box(
          modifier =
            Modifier.fillMaxSize()
              .sharedBounds(
                sharedContentState =
                  rememberSharedContentState(key = PetImageBoundsKey(state.id, state.index)),
                animatedVisibilityScope = requireAnimatedScope(Overlay),
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(easing = LinearEasing)),
                resizeMode = RemeasureToBounds,
              ),
          contentAlignment = Alignment.Center,
        ) {
          val context = LocalPlatformContext.current
          AsyncImage(
            model =
              Builder(context)
                .data(state.url)
                .apply {
                  state.placeholderKey?.let { placeholderMemoryCacheKey(it) }
                  crossfade(AnimationConstants.DefaultDurationMillis)
                }
                .build(),
            contentDescription = "Full size image",
            modifier =
              Modifier.fillMaxSize()
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                ) { showChrome = !showChrome }
                .sharedElement(
                  sharedContentState =
                    rememberSharedContentState(key = PetImageElementKey(state.url)),
                  animatedVisibilityScope = requireAnimatedScope(Overlay),
                ),
            contentScale = ContentScale.Fit,
            imageLoader = SingletonImageLoader.get(context),
          )
        }

        // Chrome (back button)
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
                subtitleContentColor = Color.Transparent,
              ),
          )
        }
      }
    }
  }
}
