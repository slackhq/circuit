// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petdetail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.ColorImage
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.sample.coil.test.CoilRule
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import com.slack.circuit.star.common.Strings
import com.slack.circuit.star.petdetail.PetDetailScreen.Event
import com.slack.circuit.star.petdetail.PetDetailScreen.Event.ViewFullBio
import com.slack.circuit.star.petdetail.PetDetailScreen.State.Full
import com.slack.circuit.star.petdetail.PetDetailTestConstants.ANIMAL_CONTAINER_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.COMPACT_CLOSE_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.COMPACT_DETAILS_PANE_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.COMPACT_PHOTO_PANE_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.DESCRIPTION_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.FULL_BIO_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.PET_NAME_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.PROGRESS_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.UNKNOWN_ANIMAL_TAG
import com.slack.circuit.star.petdetail.PetPhotoCarouselTestConstants.CAROUSEL_TAG
import com.slack.circuit.star.petdetail.PetPhotoCarouselTestConstants.PAGER_INDICATOR_TAG
import com.slack.circuit.star.ui.StarTheme
import com.slack.circuit.test.TestEventSink
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PetDetailUiTest {
  @get:Rule val composeTestRule = createComposeRule()
  @get:Rule val coilRule = CoilRule(ColorImage(Color.Blue.toArgb()))

  // TODO this seems like not the greatest test pattern, maybe something we can offer better
  //  solutions for via semantics.
  private var carouselScreen: PetPhotoCarouselScreen? = null
  private val circuit =
    Circuit.Builder()
      .setOnUnavailableContent { screen, modifier ->
        when (screen) {
          is PetPhotoCarouselScreen -> {
            PetPhotoCarousel(screen, modifier)
            carouselScreen = screen
          }
        }
      }
      .build()

  @Test
  fun petDetail_show_progress_indicator_for_loading_state() {
    composeTestRule.run {
      setTestContent(circuit) { PetDetail(PetDetailScreen.State.Loading) }

      onNodeWithTag(PROGRESS_TAG).assertIsDisplayed()
      onNodeWithTag(UNKNOWN_ANIMAL_TAG).assertDoesNotExist()
      onNodeWithTag(ANIMAL_CONTAINER_TAG).assertDoesNotExist()
    }
  }

  @Test
  fun petDetail_show_message_for_unknown_animal_state() {
    composeTestRule.run {
      setTestContent(circuit) { PetDetail(PetDetailScreen.State.UnknownAnimal) }

      onNodeWithTag(PROGRESS_TAG).assertDoesNotExist()
      onNodeWithTag(ANIMAL_CONTAINER_TAG).assertDoesNotExist()

      onNodeWithTag(UNKNOWN_ANIMAL_TAG)
        .assertIsDisplayed()
        .assertTextEquals(Strings.UNKNOWN_ANIMALS)
    }
  }

  @Test
  fun petDetail_show_animal_for_success_state() {
    val success =
      Full(
        id = 1,
        url = "url",
        photoUrls = listOf("http://some.url"),
        photoUrlMemoryCacheKey = null,
        name = "Baxter",
        descriptionMarkdown = "Grumpy looking Australian Terrier",
        tags = listOf("dog", "terrier", "male"),
        attributes = emptyList(),
        photoAspectRatio = 1.33f,
        eventSink = {},
      )

    val expectedScreen =
      PetPhotoCarouselScreen(
        id = 1,
        name = success.name,
        photoUrls = success.photoUrls,
        photoUrlMemoryCacheKey = null,
        photoAspectRatio = 1.33f,
      )

    composeTestRule.run {
      setTestContent(circuit) { ContentWithOverlays { PetDetail(success) } }

      onNodeWithTag(PROGRESS_TAG).assertDoesNotExist()
      onNodeWithTag(UNKNOWN_ANIMAL_TAG).assertDoesNotExist()

      onNodeWithTag(CAROUSEL_TAG).assertIsDisplayed().performTouchInput { swipeUp() }
      onNodeWithText(success.name).assertIsDisplayed()
      // Markdown renderer displays text differently, just check it exists
      onNodeWithText("Grumpy looking Australian Terrier").assertIsDisplayed()

      assertThat(carouselScreen).run {
        isNotNull()
        isEqualTo(expectedScreen)
      }
    }
  }

  @Test
  @Config(qualifiers = "w640dp-h360dp-land")
  fun petPhotoCarousel_keeps_paging_indicator_visible_when_height_is_constrained() {
    val screen =
      PetPhotoCarouselScreen(
        id = 1,
        name = "Baxter",
        photoUrls = listOf("http://some.url/1", "http://some.url/2"),
        photoUrlMemoryCacheKey = null,
      )

    composeTestRule.run {
      setTestContent(circuit) {
        ContentWithOverlays {
          Box(Modifier.size(width = 400.dp, height = 240.dp).clipToBounds()) {
            PetPhotoCarousel(screen, Modifier.fillMaxSize())
          }
        }
      }

      onNodeWithTag(PAGER_INDICATOR_TAG).assertIsDisplayed()
    }
  }

  @Test
  @Config(qualifiers = "w640dp-h360dp-land")
  fun petDetail_usesCompactLandscapeLayout() {
    val success =
      Full(
        id = 1,
        url = "url",
        photoUrls = listOf("http://some.url/1", "http://some.url/2"),
        photoUrlMemoryCacheKey = null,
        name = "Baxter",
        descriptionMarkdown = "**Their Foster Parent Says:** \"Baxter is very sweet.\"",
        tags = listOf("dog", "terrier", "male"),
        attributes = emptyList(),
        photoAspectRatio = 1.33f,
        eventSink = {},
      )

    composeTestRule.run {
      setTestContent(circuit) { ContentWithOverlays { PetDetail(success) } }

      val photoPane = onNodeWithTag(COMPACT_PHOTO_PANE_TAG).assertIsDisplayed()
      val detailsPane = onNodeWithTag(COMPACT_DETAILS_PANE_TAG).assertIsDisplayed()
      val close = onNodeWithTag(COMPACT_CLOSE_TAG).assertIsDisplayed()
      val name = onNodeWithTag(PET_NAME_TAG).assertIsDisplayed()
      val description = onNodeWithTag(DESCRIPTION_TAG).assertIsDisplayed()
      onNodeWithContentDescription("Close").assertIsDisplayed()
      onNodeWithTag(PAGER_INDICATOR_TAG).assertIsDisplayed()
      onNodeWithText("Their Foster Parent Says:", substring = true).assertIsDisplayed()
      onNodeWithText("**", substring = true).assertDoesNotExist()

      val photoBounds = photoPane.fetchSemanticsNode().boundsInRoot
      val detailsBounds = detailsPane.fetchSemanticsNode().boundsInRoot
      val closeBounds = close.fetchSemanticsNode().boundsInRoot
      val nameBounds = name.fetchSemanticsNode().boundsInRoot
      val descriptionBounds = description.fetchSemanticsNode().boundsInRoot
      assertThat(photoBounds.right).isAtMost(detailsBounds.left)
      assertThat(closeBounds.left).isAtLeast(photoBounds.left)
      assertThat(closeBounds.right).isAtMost(photoBounds.right)
      assertThat(nameBounds.left).isAtLeast(detailsBounds.left)
      assertThat(nameBounds.top).isLessThan(descriptionBounds.top)
    }
  }

  @Test
  @Config(qualifiers = "w640dp-h360dp-land")
  fun petDetail_scrollsCompactLandscapeDetails() {
    val success =
      Full(
        id = 1,
        url = "url",
        photoUrls = listOf("http://some.url"),
        photoUrlMemoryCacheKey = null,
        name = "Baxter",
        descriptionMarkdown =
          buildString {
            repeat(30) { append("Baxter is looking for a quiet home and a patient family.\n\n") }
          },
        tags = listOf("dog"),
        attributes = emptyList(),
        photoAspectRatio = 1.33f,
        eventSink = {},
      )

    composeTestRule.run {
      setTestContent(circuit) { ContentWithOverlays { PetDetail(success) } }

      onNodeWithTag(FULL_BIO_TAG, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    }
  }

  @Test
  @Config(qualifiers = "w800dp-h600dp-land")
  fun petDetail_usesRegularLandscapeLayoutWhenHeightIsNotCompact() {
    val success =
      Full(
        id = 1,
        url = "url",
        photoUrls = listOf("http://some.url"),
        photoUrlMemoryCacheKey = null,
        name = "Baxter",
        descriptionMarkdown = "Baxter is very sweet.",
        tags = listOf("dog"),
        attributes = emptyList(),
        photoAspectRatio = 1.33f,
        eventSink = {},
      )

    composeTestRule.run {
      setTestContent(circuit) { ContentWithOverlays { PetDetail(success) } }

      onNodeWithTag(COMPACT_PHOTO_PANE_TAG).assertDoesNotExist()
      onNodeWithTag(COMPACT_DETAILS_PANE_TAG).assertDoesNotExist()
      onNodeWithTag(COMPACT_CLOSE_TAG).assertDoesNotExist()
      onNodeWithTag(PET_NAME_TAG).assertIsDisplayed()
      onNodeWithTag(DESCRIPTION_TAG).assertIsDisplayed()
    }
  }

  @Test
  fun petDescriptionMarkdown_linkInheritsBodyTypography() {
    var typography: com.mikepenz.markdown.model.MarkdownTypography? = null
    var expectedLinkColor = Color.Unspecified

    composeTestRule.run {
      setContent {
        StarTheme {
          expectedLinkColor = MaterialTheme.colorScheme.secondary
          typography = petDescriptionMarkdownTypography()
        }
      }

      runOnIdle {
        val actualTypography = checkNotNull(typography)
        val body = actualTypography.paragraph.toSpanStyle()
        val link = checkNotNull(actualTypography.textLink.style)
        assertThat(link.fontSize).isEqualTo(body.fontSize)
        assertThat(link.letterSpacing).isEqualTo(body.letterSpacing)
        assertThat(link.fontFamily).isEqualTo(body.fontFamily)
        assertThat(link.fontWeight).isEqualTo(body.fontWeight)
        assertThat(link.color).isEqualTo(expectedLinkColor)
        assertThat(link.textDecoration).isEqualTo(TextDecoration.Underline)
      }
    }
  }

  @Test
  fun petDetail_emits_event_when_tapping_on_full_bio_button() = runTest {
    val testSink = TestEventSink<Event>()

    val success =
      Full(
        id = 1,
        url = "url",
        photoUrls = listOf("http://some.url"),
        photoUrlMemoryCacheKey = null,
        name = "Baxter",
        descriptionMarkdown = "Grumpy looking Australian Terrier",
        tags = listOf("dog", "terrier", "male"),
        attributes = emptyList(),
        photoAspectRatio = 1.33f,
        eventSink = testSink,
      )

    val circuit =
      Circuit.Builder()
        .setOnUnavailableContent { screen, modifier ->
          PetPhotoCarousel(screen as PetPhotoCarouselScreen, modifier)
        }
        .build()

    composeTestRule.run {
      setTestContent(circuit) { ContentWithOverlays { PetDetail(success) } }

      onNodeWithTag(CAROUSEL_TAG).assertIsDisplayed().performTouchInput { swipeUp() }
      onNodeWithTag(FULL_BIO_TAG, true).assertIsDisplayed().performClick()

      testSink.assertEvent(ViewFullBio(success.url))
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
private fun ComposeContentTestRule.setTestContent(
  circuit: Circuit,
  content: @Composable () -> Unit,
) {
  setContent {
    PreviewSharedElementTransitionLayout { CircuitCompositionLocals(circuit) { content() } }
  }
}
