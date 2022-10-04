/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit.star.petdetail

import android.content.res.Configuration
import android.view.KeyEvent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.calculateCurrentOffsetForPage
import com.google.accompanist.pager.rememberPagerState
import com.slack.circuit.CircuitConfig
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.ScreenUi
import com.slack.circuit.Ui
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.petdetail.PetPhotoCarouselTestConstants.CAROUSEL_TAG
import com.slack.circuit.ui
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Inject
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

/*
 * This is a trivial example of a photo carousel used in the pet detail screen. We'd normally likely
 * write this UI directly in the detail screen UI itself, but it's helpful for demonstrating
 * nested Circuit UIs in this sample app.
 *
 * This differs from some other screens by only displaying the input screen directly as static
 * state, as opposed to reading from a repository or maintaining any sort of produced state.
 */

// We're using the screen key as the state as it's all static
@Parcelize
data class PetPhotoCarouselScreen(
  val name: String,
  val photoUrls: List<String>,
  val photoUrlMemoryCacheKey: String?,
) : Screen {
  data class State(val input: PetPhotoCarouselScreen) : CircuitUiState
}

// TODO can we make a StaticStatePresenter for cases like this? Maybe even generate _from_ the
//  screen type?
class PetPhotoCarouselPresenter
@AssistedInject
constructor(@Assisted private val screen: PetPhotoCarouselScreen) :
  Presenter<PetPhotoCarouselScreen.State> {

  @Composable override fun present() = PetPhotoCarouselScreen.State(screen)

  @AssistedFactory
  interface Factory {
    fun create(screen: PetPhotoCarouselScreen): PetPhotoCarouselPresenter
  }
}

@ContributesMultibinding(AppScope::class)
class PetPhotoCarouselUiFactory @Inject constructor() : Ui.Factory {
  override fun create(screen: Screen, circuitConfig: CircuitConfig): ScreenUi? {
    return when (screen) {
      is PetPhotoCarouselScreen -> ScreenUi(petPhotoCarousel())
      else -> null
    }
  }
}

fun petPhotoCarousel(): Ui<PetPhotoCarouselScreen.State> = ui { state -> PetPhotoCarousel(state) }

internal object PetPhotoCarouselTestConstants {
  const val CAROUSEL_TAG = "carousel"
}

@OptIn(ExperimentalPagerApi::class)
@Composable
internal fun PetPhotoCarousel(state: PetPhotoCarouselScreen.State) {
  val (name, photoUrls, photoUrlMemoryCacheKey) = state.input
  val context = LocalContext.current
  val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
  // Prefetch images
  LaunchedEffect(Unit) {
    for (url in photoUrls) {
      if (url.isBlank()) continue
      val request = ImageRequest.Builder(context).data(url).build()
      context.imageLoader.enqueue(request)
    }
  }

  val totalPhotos = photoUrls.size
  val pagerState = rememberPagerState()
  val scope = rememberCoroutineScope()
  val requester = remember { FocusRequester() }
  val columnModifier = if (isLandscape) Modifier.fillMaxWidth(0.5f) else Modifier.fillMaxSize()
  Column(
    columnModifier
      .testTag(CAROUSEL_TAG)
      // Some images are different sizes. We probably want to constrain them to the same common
      // size though
      .animateContentSize()
      .focusRequester(requester)
      .focusable()
      .onKeyEvent { event ->
        if (event.nativeKeyEvent.action != KeyEvent.ACTION_UP) return@onKeyEvent false
        val index =
          when (event.nativeKeyEvent.keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
              pagerState.currentPage.inc().takeUnless { it >= totalPhotos } ?: -1
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
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
    HorizontalPager(
      count = totalPhotos,
      state = pagerState,
      key = photoUrls::get,
      contentPadding = PaddingValues(16.dp),
    ) { page ->
      Card(
        modifier =
          Modifier.graphicsLayer {
            // Calculate the absolute offset for the current page from the
            // scroll position. We use the absolute value which allows us to mirror
            // any effects for both directions
            val pageOffset = calculateCurrentOffsetForPage(page).absoluteValue

            // We animate the scaleX + scaleY, between 85% and 100%
            lerp(start = 0.85f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f)).also { scale
              ->
              scaleX = scale
              scaleY = scale
            }

            // We animate the alpha, between 50% and 100%
            alpha = lerp(start = 0.5f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))
          }
      ) {
        AsyncImage(
          modifier = Modifier.fillMaxWidth(),
          model =
            ImageRequest.Builder(LocalContext.current)
              .data(photoUrls[page].takeIf(String::isNotBlank))
              .apply {
                if (page == 0) {
                  placeholderMemoryCacheKey(photoUrlMemoryCacheKey)
                  crossfade(true)
                }
              }
              .build(),
          contentDescription = name,
          contentScale = ContentScale.FillWidth,
        )
      }
    }

    HorizontalPagerIndicator(
      pagerState = pagerState,
      modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
      activeColor = MaterialTheme.colorScheme.onBackground
    )
  }

  // Focus the pager so we can cycle through it with arrow keys
  LaunchedEffect(Unit) { requester.requestFocus() }
}
