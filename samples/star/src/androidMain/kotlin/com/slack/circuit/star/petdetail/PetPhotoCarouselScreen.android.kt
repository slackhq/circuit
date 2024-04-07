package com.slack.circuit.star.petdetail

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest.Builder
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.internal.rememberStableCoroutineScope
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.petdetail.PetPhotoCarouselScreen.State
import com.slack.circuit.star.petdetail.PetPhotoCarouselTestConstants.CAROUSEL_TAG
import com.slack.circuit.star.ui.HorizontalPagerIndicator
import com.slack.circuit.star.ui.SharedElementTransitionScope
import com.slack.circuit.star.ui.sharedElementAnimatedContentScope
import kotlinx.coroutines.launch

@OptIn(
  ExperimentalSharedTransitionApi::class,
  ExperimentalMaterial3WindowSizeClassApi::class,
  ExperimentalFoundationApi::class,
  ExperimentalAnimationApi::class,
)
@Composable
@CircuitInject(PetPhotoCarouselScreen::class, AppScope::class)
actual fun PetPhotoCarousel(state: State, modifier: Modifier) = SharedElementTransitionScope {
  val (id, name, photoUrls, photoUrlMemoryCacheKey) = state
  val context = LocalPlatformContext.current
  // Prefetch images
  LaunchedEffect(Unit) {
    for (url in photoUrls) {
      if (url.isBlank()) continue
      val request = Builder(context).data(url).build()
      SingletonImageLoader.get(context).enqueue(request)
    }
  }

  val totalPhotos = photoUrls.size
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
  val boundsTransform = { _: Rect, _: Rect -> tween<Rect>(1400) }
  Column(
    columnModifier
      .testTag(CAROUSEL_TAG)
      // Some images are different sizes. We probably want to constrain them to the same common
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
      pagerState = pagerState,
      photoUrls = photoUrls,
      name = name,
      photoUrlMemoryCacheKey = photoUrlMemoryCacheKey,
      modifier =
        Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(key = "animal-${id}"),
            animatedVisibilityScope = sharedElementAnimatedContentScope(),
            boundsTransform = boundsTransform,
          )
          .sharedElement(
            state = rememberSharedContentState(key = "animal-image-${id}"),
            animatedVisibilityScope = sharedElementAnimatedContentScope(),
          ),
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
