// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.slack.circuit.internal.test.TestContentTags

internal fun SemanticsNodeInteractionsProvider.onTopNavigationRecordNodeWithTag(
  testTag: String
): SemanticsNodeInteraction =
  onAllNodes(hasTestTag(testTag) and hasParent(hasTestTag(TestContentTags.TAG_ROOT)), false)
    // first is always on top
    .onFirst()

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.swipeRight() {
  onRoot().performTouchInput { swipeRight(startX = width * 0.1f, endX = width * 0.8f) }
}
