// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuit.star.animation.HomeAnimatedScreenTransform
import com.slack.circuit.star.animation.PetDetailAnimatedScreenTransform
import com.slack.circuit.star.di.AppGraph
import com.slack.circuit.star.home.HomeScreen
import com.slack.circuit.star.petdetail.PetDetailScreen
import com.slack.circuit.star.ui.StarTheme
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory
import kotlinx.collections.immutable.persistentListOf
import platform.UIKit.UIViewController

@OptIn(ExperimentalCircuitApi::class, ExperimentalSharedTransitionApi::class)
fun makeUiViewController(graph: AppGraph): UIViewController = ComposeUIViewController {
  val circuit =
    graph.circuit
      .newBuilder()
      .addAnimatedScreenTransforms(
        HomeScreen::class to HomeAnimatedScreenTransform,
        PetDetailScreen::class to PetDetailAnimatedScreenTransform,
      )
      .build()

  StarTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
      val backStack = rememberSaveableBackStack(persistentListOf(HomeScreen))
      val circuitNavigator = rememberCircuitNavigator(backStack, onRootPop = {})
      CircuitCompositionLocals(circuit) {
        SharedElementTransitionLayout {
          ContentWithOverlays {
            NavigableCircuitContent(
              navigator = circuitNavigator,
              backStack = backStack,
              decoratorFactory =
                remember(circuitNavigator) {
                  GestureNavigationDecorationFactory(onBackInvoked = circuitNavigator::pop)
                },
            )
          }
        }
      }
    }
  }
}
