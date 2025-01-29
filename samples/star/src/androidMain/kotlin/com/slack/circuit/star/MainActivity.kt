// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_LIGHT
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuit.star.benchmark.ListBenchmarksScreen
import com.slack.circuit.star.di.ActivityKey
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.home.HomeScreen
import com.slack.circuit.star.navigation.OpenUrlScreen
import com.slack.circuit.star.petdetail.PetDetailScreen
import com.slack.circuit.star.ui.StarTheme
import com.slack.circuitx.android.AndroidScreen
import com.slack.circuitx.android.IntentScreen
import com.slack.circuitx.android.rememberAndroidScreenAwareNavigator
import com.slack.circuitx.gesturenavigation.GestureNavigationDecoration
import dev.zacsweers.metro.BoundType
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.persistentListOf
import okhttp3.HttpUrl.Companion.toHttpUrl

@ContributesIntoMap(AppScope::class, boundType = BoundType<Activity>())
@ActivityKey(MainActivity::class)
@Inject
class MainActivity(private val circuit: Circuit) : AppCompatActivity() {

  @OptIn(ExperimentalSharedTransitionApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val initialBackstack =
      if (intent.getBooleanExtra("benchmark", false)) {
        val scenario = intent.getStringExtra("scenario") ?: error("Missing scenario")
        when (scenario) {
          "list" -> {
            val useNestedContent = intent.getBooleanExtra("useNestedContent", false)
            persistentListOf(ListBenchmarksScreen(useNestedContent))
          }
          else -> error("Unknown scenario: $scenario")
        }
      } else if (intent.data == null) {
        persistentListOf(HomeScreen)
      } else {
        val httpUrl = intent.data.toString().toHttpUrl()
        val animalId = httpUrl.pathSegments[1].substringAfterLast("-").toLong()
        val petDetailScreen =
          PetDetailScreen(petId = animalId, photoUrlMemoryCacheKey = null, animal = null)
        persistentListOf(HomeScreen, petDetailScreen)
      }

    setContent {
      StarTheme {
        // TODO why isn't the windowBackground enough so we don't need to do this?
        Surface(color = MaterialTheme.colorScheme.background) {
          val backStack = rememberSaveableBackStack(initialBackstack)
          val circuitNavigator = rememberCircuitNavigator(backStack)
          val navigator = rememberAndroidScreenAwareNavigator(circuitNavigator, this::goTo)
          CircuitCompositionLocals(circuit) {
            SharedElementTransitionLayout {
              ContentWithOverlays {
                NavigableCircuitContent(
                  navigator = navigator,
                  backStack = backStack,
                  decoration = GestureNavigationDecoration(onBackInvoked = navigator::pop),
                )
              }
            }
          }
        }
      }
    }
  }

  private fun goTo(screen: AndroidScreen) =
    when (screen) {
      is OpenUrlScreen -> goTo(screen)
      is IntentScreen -> screen.startWith(this)
      else -> error("Unknown AndroidScreen: $screen")
    }

  private fun goTo(screen: OpenUrlScreen): Boolean {
    val scheme = CustomTabColorSchemeParams.Builder().setToolbarColor(0x000000).build()
    CustomTabsIntent.Builder()
      .setColorSchemeParams(COLOR_SCHEME_LIGHT, scheme)
      .setColorSchemeParams(COLOR_SCHEME_DARK, scheme)
      .setShowTitle(true)
      .build()
      .launchUrl(this, Uri.parse(screen.url))
    return true
  }
}
