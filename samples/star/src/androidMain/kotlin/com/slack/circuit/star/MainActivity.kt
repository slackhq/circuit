// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_LIGHT
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.star.benchmark.ListBenchmarksScreen
import com.slack.circuit.star.di.ActivityKey
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.home.HomeScreen
import com.slack.circuit.star.imageviewer.ImageViewerAwareNavDecoration
import com.slack.circuit.star.navigation.OpenUrlScreen
import com.slack.circuit.star.petdetail.PetDetailScreen
import com.slack.circuit.star.ui.StarTheme
import com.slack.circuitx.android.AndroidScreen
import com.slack.circuitx.android.IntentScreen
import com.slack.circuitx.gesturenavigation.GestureNavigationDecoration
import com.slack.circuitx.navigation.intercepting.AndroidScreenAwareNavigationInterceptor
import com.slack.circuitx.navigation.intercepting.CircuitInterceptingNavigator
import com.slack.circuitx.navigation.intercepting.CircuitNavigationInterceptor
import com.slack.circuitx.navigation.intercepting.LoggingNavigationEventListener
import com.slack.circuitx.navigation.intercepting.rememberCircuitInterceptingNavigator
import com.squareup.anvil.annotations.ContributesMultibinding
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import okhttp3.HttpUrl.Companion.toHttpUrl

@ContributesMultibinding(AppScope::class, boundType = Activity::class)
@ActivityKey(MainActivity::class)
class MainActivity @Inject constructor(private val circuit: Circuit) : AppCompatActivity() {

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
        val petDetailScreen = PetDetailScreen(animalId, null)
        persistentListOf(HomeScreen, petDetailScreen)
      }

    val interceptors = persistentListOf(AndroidScreenAwareNavigationInterceptor(this::goTo))
    val eventListeners = persistentListOf(LoggingNavigationEventListener)
    val notifier =
      object : CircuitInterceptingNavigator.FailureNotifier {
        override fun goToInterceptorFailure(result: CircuitNavigationInterceptor.Result.Failure) {
          Log.w("Circuit", "goToInterceptorFailure: $result")
        }

        override fun popInterceptorFailure(result: CircuitNavigationInterceptor.Result.Failure) {
          Log.w("Circuit", "popInterceptorFailure: $result")
        }
      }

    setContent {
      StarTheme {
        // TODO why isn't the windowBackground enough so we don't need to do this?
        Surface(color = MaterialTheme.colorScheme.background) {
          val backStack = rememberSaveableBackStack(initialBackstack)
          // Build the delegate Navigator.
          val interceptingNavigator =
            rememberCircuitInterceptingNavigator(
              backStack = backStack,
              interceptors = interceptors,
              eventListeners = eventListeners,
              notifier = notifier,
            )
          CircuitCompositionLocals(circuit) {
            ContentWithOverlays {
              NavigableCircuitContent(
                navigator = interceptingNavigator,
                backStack = backStack,
                decoration =
                  ImageViewerAwareNavDecoration(
                    GestureNavigationDecoration(onBackInvoked = interceptingNavigator::pop)
                  ),
              )
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
