// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petdetail

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize.Companion.animatedSize
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.EaseInExpo
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest.Builder
import coil3.request.crossfade
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.thenIfNotNull
import com.slack.circuit.overlay.LocalOverlayState
import com.slack.circuit.overlay.OverlayEffect
import com.slack.circuit.overlay.OverlayState.UNAVAILABLE
import com.slack.circuit.runtime.internal.rememberStableCoroutineScope
import com.slack.circuit.runtime.screen.StaticScreen
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Overlay
import com.slack.circuit.sharedelements.requireActiveAnimatedScope
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.imageviewer.ImageViewerScreen
import com.slack.circuit.star.parcel.CommonParcelize
import com.slack.circuit.star.petdetail.PetPhotoCarouselTestConstants.CAROUSEL_TAG
import com.slack.circuit.star.transition.PetImageBoundsKey
import com.slack.circuit.star.transition.PetImageElementKey
import com.slack.circuit.star.ui.HorizontalPagerIndicator
import com.slack.circuitx.overlays.showFullScreenOverlay
import kotlin.math.absoluteValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@CommonParcelize
data class PetPhotoCarouselScreen(
  val id: Long,
  val name: String,
  val photoUrls: ImmutableList<String>,
  val photoUrlMemoryCacheKey: String?,
) : StaticScreen

/*
 * This is a trivial example of a photo carousel used in the pet detail screen. We'd normally likely
 * write this UI directly in the detail screen UI itself, but it's helpful for demonstrating
 * nested Circuit UIs in this sample app.
 *
 * This differs from some other screens by only displaying the input screen directly as static
 * state, as opposed to reading from a repository or maintaining any sort of produced state.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalSharedTransitionApi::class)
@CircuitInject(PetPhotoCarouselScreen::class, AppScope::class)
@Composable
internal fun PetPhotoCarousel(screen: PetPhotoCarouselScreen, modifier: Modifier = Modifier) {
  val context = LocalPlatformContext.current
  // Prefetch images
  LaunchedEffect(Unit) {
    for (url in screen.photoUrls) {
      if (url.isBlank()) continue
      val request = Builder(context).data(url).build()
      SingletonImageLoader.get(context).enqueue(request)
    }
  }

  val totalPhotos = screen.photoUrls.size
  val pagerState = rememberPagerState { totalPhotos }
  val scope = rememberStableCoroutineScope()
  val requester = remember { FocusRequester() }
  @Suppress("MagicNumber")
  val columnModifier =
    when (calculateWindowSizeClass().widthSizeClass) {
      WindowWidthSizeClass.Medium,
      WindowWidthSizeClass.Expanded -> modifier.fillMaxWidth(0.5f)
      else -> modifier.fillMaxSize()
    }
  Column(
    columnModifier
      .testTag(CAROUSEL_TAG)
      // Some images are different sizes. We probably want to constrain them to the same
      // common
      // size though
      .animateContentSize()
      .focusRequester(requester)
      .focusable()
      .onKeyEvent { event ->
        if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
        val index =
          when (event.key) {
            Key.DirectionRight -> {
              pagerState.currentPage.inc().takeUnless { it >= totalPhotos } ?: -1
            }
            Key.DirectionLeft -> {
              pagerState.currentPage.dec().takeUnless { it < 0 } ?: -1
            }
            else -> -1
          }
        if (index == -1) {
          false
        } else {
          scope.launch { pagerState.animateScrollToPage(index) }
          true
        }
      }
  ) {
    PhotoPager(
      id = screen.id,
      pagerState = pagerState,
      photoUrls = screen.photoUrls,
      name = screen.name,
      photoUrlMemoryCacheKey = screen.photoUrlMemoryCacheKey,
      modifier = Modifier,
    )

    HorizontalPagerIndicator(
      pagerState = pagerState,
      pageCount = totalPhotos,
      modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
      activeColor = MaterialTheme.colorScheme.onBackground,
    )
  }

  // Focus the pager so we can cycle through it with arrow keys
  LaunchedEffect(Unit) { requester.requestFocus() }
}

private fun PagerState.calculateCurrentOffsetForPage(page: Int): Float {
  return (currentPage - page) + currentPageOffsetFraction
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Suppress("LongParameterList")
@Composable
private fun PhotoPager(
  id: Long,
  pagerState: PagerState,
  photoUrls: ImmutableList<String>,
  name: String,
  modifier: Modifier = Modifier,
  photoUrlMemoryCacheKey: String? = null,
) = SharedElementTransitionScope {
  HorizontalPager(
    state = pagerState,
    key = photoUrls::get,
    modifier = modifier,
    contentPadding = PaddingValues(16.dp),
  ) { page ->
    val photoUrl by remember { derivedStateOf { photoUrls[page].takeIf(String::isNotBlank) } }
    var shownOverlayUrl by remember { mutableStateOf<String?>(null) }

    OverlayEffect(shownOverlayUrl) {
      shownOverlayUrl?.let { url ->
        showFullScreenOverlay(
          ImageViewerScreen(id = id, url = url, index = page, placeholderKey = url)
        )
        shownOverlayUrl = null
      }
    }

    val shape = CardDefaults.shape
    // TODO implement full screen overlay on non-android targets
    val animatedVisibilityScope = requireActiveAnimatedScope()
    val navScope = requireAnimatedScope(Navigation)
    val exitingToList =
      animatedVisibilityScope == navScope &&
        animatedVisibilityScope.transition.targetState == EnterExitState.PostExit
    val clickableModifier =
      when {
          exitingToList && page != 0 ->
            with(animatedVisibilityScope) {
              Modifier.animateEnterExit(
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(durationMillis = 0, easing = EaseOutExpo)),
              )
            }
          pagerState.currentPage == page ->
            Modifier.sharedBounds(
              sharedContentState = rememberSharedContentState(key = PetImageBoundsKey(id, page)),
              animatedVisibilityScope = animatedVisibilityScope,
              placeHolderSize = animatedSize,
              resizeMode = RemeasureToBounds,
              enter = fadeIn(animationSpec = tween(durationMillis = 20, easing = EaseInExpo)),
              exit = fadeOut(animationSpec = tween(durationMillis = 20, easing = EaseOutExpo)),
              zIndexInOverlay = 3f,
            )
          else -> Modifier
        }
        .clip(shape)
        .clickable(enabled = LocalOverlayState.current != UNAVAILABLE && photoUrl != null) {
          shownOverlayUrl = photoUrl
        }
    Card(
      shape = shape,
      modifier =
        clickableModifier.aspectRatio(1f).graphicsLayer {
          // Calculate the absolute offset for the current page from the
          // scroll position. We use the absolute value which allows us to mirror
          // any effects for both directions
          val pageOffset = pagerState.calculateCurrentOffsetForPage(page).absoluteValue

          // We animate the scaleX + scaleY, between 85% and 100%
          lerp(start = 0.85f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f)).also { scale
            ->
            scaleX = scale
            scaleY = scale
          }

          // We animate the alpha, between 50% and 100%
          alpha = lerp(start = 0.5f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))
        },
    ) {
      AsyncImage(
        modifier =
          Modifier.fillMaxWidth().thenIfNotNull(photoUrl) {
            sharedElement(
                state = rememberSharedContentState(key = PetImageElementKey(it)),
                animatedVisibilityScope = requireAnimatedScope(Overlay),
                zIndexInOverlay = 5f,
              )
              .clip(shape)
          },
        model =
          Builder(LocalPlatformContext.current)
            .data(photoUrl)
            .memoryCacheKey(photoUrl)
            .apply {
              if (page == 0) {
                placeholderMemoryCacheKey(photoUrlMemoryCacheKey)
                crossfade(AnimationConstants.DefaultDurationMillis)
              }
            }
            .build(),
        contentDescription = name,
        contentScale = ContentScale.Crop,
        imageLoader = SingletonImageLoader.get(LocalPlatformContext.current),
      )
    }
  }
}

internal object PetPhotoCarouselTestConstants {
  const val CAROUSEL_TAG = "carousel"
}
