// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sharedelements

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Overlay
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalTestApi::class)
class ProvideStaticAnimatedTransitionScopeTest {

  @Test
  fun providesTheRequestedScope() = runComposeUiTest {
    var navigation: AnimatedVisibilityScope? = null
    var overlay: AnimatedVisibilityScope? = null
    setContent {
      SharedElementTransitionLayout {
        ProvideStaticAnimatedTransitionScope(Navigation) {
          SharedElementTransitionScope {
            navigation = requireAnimatedScope(Navigation)
            overlay = findAnimatedScope(Overlay)
          }
        }
      }
    }
    waitForIdle()
    assertNotNull(navigation)
    assertNull(overlay)
  }

  @Test
  fun showsContentWithoutASharedElementLayout() = runComposeUiTest {
    var composed = false
    setContent { ProvideStaticAnimatedTransitionScope(Navigation) { composed = true } }
    waitForIdle()
    assertTrue(composed)
  }
}
