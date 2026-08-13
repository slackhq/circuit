// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petdetail

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.slack.circuit.star.db.Animal
import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.Size
import com.slack.circuit.star.petdetail.PetDetailScreen.State.Full
import kotlin.test.Test

class PetDescriptionMarkdownTest {
  @Test
  fun malformedDescriptionIsNormalizedWhenMappedToDetailState() {
    val markdown = """**Their Foster Parent Says: "**Ted is very sweet."""
    val animal =
      Animal(
        id = 1L,
        sort = 0L,
        name = "Ted",
        url = "https://example.com",
        photos = emptyList(),
        tags = emptyList(),
        description = null,
        descriptionMarkdown = markdown,
        attributes = emptyList(),
        primaryBreed = null,
        gender = Gender.MALE,
        size = Size.SMALL,
        age = null,
      )

    val state = animal.toPetDetailState(photoUrlMemoryCacheKey = null, eventSink = {}) as Full

    assertThat(state.descriptionMarkdown)
      .isEqualTo("""**Their Foster Parent Says:** "Ted is very sweet.""")
  }

  @Test
  fun curlyOpeningQuoteIsMovedOutsideBoldLabel() {
    val markdown = """**Their Foster Parent Says: “**Ted is very sweet."""

    assertThat(markdown.normalizePetDescriptionMarkdown())
      .isEqualTo("""**Their Foster Parent Says:** “Ted is very sweet.""")
  }

  @Test
  fun validQuotedBoldTextIsUnchanged() {
    val markdown = """Recommended: "**quiet homes only**"""

    assertThat(markdown.normalizePetDescriptionMarkdown()).isEqualTo(markdown)
  }
}
