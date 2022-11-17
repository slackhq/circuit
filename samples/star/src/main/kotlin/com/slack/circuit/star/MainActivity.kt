// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_LIGHT
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import com.slack.circuit.CircuitCompositionLocals
import com.slack.circuit.CircuitConfig
import com.slack.circuit.NavigableCircuitContent
import com.slack.circuit.Screen
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.push
import com.slack.circuit.rememberCircuitNavigator
import com.slack.circuit.star.di.ActivityKey
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.home.HomeScreen
import com.slack.circuit.star.navigator.AndroidScreen
import com.slack.circuit.star.navigator.AndroidSupportingNavigator
import com.slack.circuit.star.petdetail.PetDetailScreen
import com.slack.circuit.star.ui.StarTheme
import com.squareup.anvil.annotations.ContributesMultibinding
import javax.inject.Inject
import okhttp3.HttpUrl.Companion.toHttpUrl

@ContributesMultibinding(AppScope::class, boundType = Activity::class)
@ActivityKey(MainActivity::class)
class MainActivity
@Inject
constructor(
  private val viewModelProviderFactory: ViewModelProvider.Factory,
  private val circuitConfig: CircuitConfig
) : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    var backStack: List<Screen> = listOf(HomeScreen)
    if (intent.data != null) {
      val httpUrl = intent.data.toString().toHttpUrl()
      val animalId = httpUrl.pathSegments[1].substringAfterLast("-").toLong()
      val petDetailScreen = PetDetailScreen(animalId, null)
      backStack = listOf(HomeScreen, petDetailScreen)
    }

    setContent {
      StarTheme {
        // TODO why isn't the windowBackground enough so we don't need to do this?
        Surface(color = MaterialTheme.colorScheme.background) {
          val backstack = rememberSaveableBackStack { backStack.forEach { screen -> push(screen) } }
          val circuitNavigator = rememberCircuitNavigator(backstack)
          val navigator =
            remember(circuitNavigator) { AndroidSupportingNavigator(circuitNavigator, this::goTo) }
          CircuitCompositionLocals(circuitConfig) {
            ContentWithOverlays { NavigableCircuitContent(navigator, backstack) }
          }
        }
      }
    }
  }

  override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
    return viewModelProviderFactory
  }

  private fun goTo(screen: AndroidScreen) =
    when (screen) {
      is AndroidScreen.CustomTabsIntentScreen -> goTo(screen)
      is AndroidScreen.IntentScreen -> TODO()
    }

  private fun goTo(screen: AndroidScreen.CustomTabsIntentScreen) {
    val scheme = CustomTabColorSchemeParams.Builder().setToolbarColor(0x000000).build()
    CustomTabsIntent.Builder()
      .setColorSchemeParams(COLOR_SCHEME_LIGHT, scheme)
      .setColorSchemeParams(COLOR_SCHEME_DARK, scheme)
      .setShowTitle(true)
      .build()
      .launchUrl(this, Uri.parse(screen.url))
  }
}
