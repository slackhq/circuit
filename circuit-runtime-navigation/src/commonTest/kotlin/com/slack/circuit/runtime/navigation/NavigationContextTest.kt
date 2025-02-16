// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.navigation

import androidx.compose.runtime.saveable.SaverScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private val initialTestTag = TestTag("Test")
private val otherTestTag = OtherTestTag("Other Test")

class NavigationContextTest {

  private val context = NavigationContext().apply { putTag(initialTestTag) }

  @Test
  fun `tag get existing tag`() {
    val actual = context.tag<TestTag>()
    assertEquals(TestTag("Test"), actual)
  }

  @Test
  fun `tag get nonexistent tag`() {
    val actual = context.tag<OtherTestTag>()
    assertNull(actual)
  }

  @Test
  fun `putTag add new tag`() {
    context.putTag(otherTestTag)
    val actual = context.tag<OtherTestTag>()
    assertEquals(otherTestTag, actual)
  }

  @Test
  fun `putTag replace existing tag`() {
    val expected = TestTag("Replace Test")
    context.putTag(expected)
    val actual = context.tag<TestTag>()
    assertEquals(expected, actual)
  }

  @Test
  fun `putTag remove tag with null tag`() {
    context.putTag<TestTag>(null)
    val actual = context.tag<TestTag>()
    assertNull(actual)
  }

  @Test
  fun `clearTags empty context`() {
    assertNotNull(context.tag<TestTag>())
    context.clearTags()
    assertNull(context.tag<TestTag>())
  }

  @Test
  fun `clearTags non empty context`() {
    context.putTag(otherTestTag)
    assertNotNull(context.tag<TestTag>())
    assertNotNull(context.tag<OtherTestTag>())
    context.clearTags()
    assertNull(context.tag<TestTag>())
    assertNull(context.tag<OtherTestTag>())
  }

  @Test
  fun `saver non serializable object`() {
    context.putTag(otherTestTag)
    val scope = SaverScope { value ->
      when (value) {
        is String -> true
        is TestTag -> true
        is OtherTestTag -> false
        else -> false
      }
    }
    val saveable = with(scope) { with(NavigationContext.Saver) { save(context) } }
    val expected = listOf(tagKey(TestTag::class), initialTestTag)
    println(expected)
    assertEquals(expected, saveable)
  }

  @Test
  fun `saver correct save restore`() {
    val saveable =
      listOf(tagKey(TestTag::class), initialTestTag, tagKey(OtherTestTag::class), otherTestTag)
    val actual = NavigationContext.Saver.restore(saveable)
    context.putTag(otherTestTag)
    assertEquals(context, actual)
  }

  @Test
  fun `saver invalid save size restore`() {
    assertFailsWith<IllegalStateException>("non-zero remainder") {
      val saveable = listOf(tagKey(TestTag::class), initialTestTag, tagKey(OtherTestTag::class))
      NavigationContext.Saver.restore(saveable)
    }
  }

  @Test
  fun `saver invalid save data restore`() {
    assertFailsWith<ClassCastException> {
      val saveable =
        listOf(tagKey(TestTag::class), tagKey(OtherTestTag::class), initialTestTag, otherTestTag)
      NavigationContext.Saver.restore(saveable)
    }
  }
}

data class TestTag(val value: String)

data class OtherTestTag(val value: String)
