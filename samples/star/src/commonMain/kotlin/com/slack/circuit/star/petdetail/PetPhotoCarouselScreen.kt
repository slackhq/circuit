// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petdetail

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.PlaceholderSize.Companion.AnimatedSize
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.EaseInExpo
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest.Builder
import coil3.request.crossfade
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.overlay.LocalOverlayState
import com.slack.circuit.overlay.OverlayEffect
import com.slack.circuit.overlay.OverlayState.UNAVAILABLE
import com.slack.circuit.runtime.internal.rememberStableCoroutineScope
import com.slack.circuit.runtime.screen.StaticScreen
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Overlay
import com.slack.circuit.sharedelements.requireActiveAnimatedScope
import com.slack.circuit.star.imageviewer.ImageViewerScreen
import com.slack.circuit.star.petdetail.PetPhotoCarouselTestConstants.CAROUSEL_TAG
import com.slack.circuit.star.transition.PetImageBoundsKey
import com.slack.circuit.star.transition.PetImageElementKey
import com.slack.circuit.star.ui.HorizontalPagerIndicator
import com.slack.circuit.star.ui.thenIfNotNull
import com.slack.circuitx.overlays.showFullScreenOverlay
import dev.zacsweers.metro.AppScope
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

/** Default aspect ratio to use when photo metadata is not available (4:3) */
private const val DEFAULT_ASPECT_RATIO = 4f / 3f

@Parcelize
data class PetPhotoCarouselScreen(
  val id: Long,
  val name: String,
  val photoUrls: List<String>,
  val photoUrlMemoryCacheKey: String?,
  val photoAspectRatio: Float? = null,
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
  val isLandscape =
    when (calculateWindowSizeClass().widthSizeClass) {
      WindowWidthSizeClass.Medium,
      WindowWidthSizeClass.Expanded -> true
      else -> false
    }
  // Use the primary photo's aspect ratio for consistent container sizing
  val containerAspectRatio = screen.photoAspectRatio ?: DEFAULT_ASPECT_RATIO

  Column(
    modifier.testTag(CAROUSEL_TAG).focusRequester(requester).focusable().onKeyEvent { event ->
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
    },
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    PhotoPager(
      id = screen.id,
      pagerState = pagerState,
      photoUrls = screen.photoUrls,
      name = screen.name,
      photoUrlMemoryCacheKey = screen.photoUrlMemoryCacheKey,
      containerAspectRatio = containerAspectRatio,
    )

    HorizontalPagerIndicator(
      pagerState = pagerState,
      pageCount = totalPhotos,
      modifier = Modifier.padding(16.dp),
      activeColor = MaterialTheme.colorScheme.onBackground,
    )
  }

  // Always request focus for keyboard navigation in landscape
  // In portrait, skip focus request to avoid issues with LazyColumn/PinnableContainer
  // https://issuetracker.google.com/issues/381270279
  if (isLandscape || LocalPinnableContainer.current == null) {
    LaunchedEffect(Unit) { requester.requestFocus() }
  }
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
  photoUrls: List<String>,
  name: String,
  containerAspectRatio: Float,
  modifier: Modifier = Modifier,
  photoUrlMemoryCacheKey: String? = null,
) = SharedElementTransitionScope {
  HorizontalPager(
    state = pagerState,
    // Use safe accessor to avoid IndexOutOfBoundsException during pager prefetching
    key = { page -> photoUrls.getOrElse(page) { page } },
    // Apply aspectRatio to give HorizontalPager a defined height (needed in LazyColumn)
    modifier = modifier.aspectRatio(containerAspectRatio),
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
    val sharedBoundsModifier =
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
            placeholderSize = AnimatedSize,
            resizeMode = RemeasureToBounds,
            enter = fadeIn(animationSpec = tween(durationMillis = 20, easing = EaseInExpo)),
            exit = fadeOut(animationSpec = tween(durationMillis = 20, easing = EaseOutExpo)),
            zIndexInOverlay = 3f,
          )
        else -> Modifier
      }
    val context = LocalPlatformContext.current
    val imageLoader = SingletonImageLoader.get(context)

    // Box centers the image and applies shared element + scaling effects to the container
    Box(
      modifier =
        Modifier.fillMaxSize().then(sharedBoundsModifier).graphicsLayer {
          val pageOffset = pagerState.calculateCurrentOffsetForPage(page).absoluteValue
          lerp(start = 0.85f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f)).also { scale
            ->
            scaleX = scale
            scaleY = scale
          }
          alpha = lerp(start = 0.5f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))
        },
      contentAlignment = Alignment.Center,
    ) {
      AsyncImage(
        model =
          Builder(context)
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
        imageLoader = imageLoader,
        modifier =
          Modifier.clip(shape)
            .fillMaxSize()
            .clickable(enabled = LocalOverlayState.current != UNAVAILABLE && photoUrl != null) {
              shownOverlayUrl = photoUrl
            }
            .thenIfNotNull(photoUrl) {
              sharedElement(
                sharedContentState = rememberSharedContentState(key = PetImageElementKey(it)),
                animatedVisibilityScope = requireAnimatedScope(Overlay),
                zIndexInOverlay = 5f,
              )
            },
        contentScale = ContentScale.Fit,
      )
    }
  }
}

internal object PetPhotoCarouselTestConstants {
  const val CAROUSEL_TAG = "carousel"
}
