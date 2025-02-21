// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.CheckResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize

/**
 * A [Screen] that refers to a start-able Android component, such as an [Intent].
 *
 * These screens are intercepted by the intercepting [Navigator] returned by
 * [rememberAndroidScreenAwareNavigator] and routed to an [AndroidScreenStarter] that can start it
 * natively, such as via [Context.startActivity].
 *
 * This is an interop layer between the [Navigator] API and the Android platform, and not something
 * that should be used to navigate between standard Circuit screens. It should be assumed that
 * calling [Navigator.goTo] with an [AndroidScreen] will result in exiting the current Circuit UI.
 */
public interface AndroidScreen : Screen

/**
 * A simple [Intent] [Screen] that wraps an [intent] and optional [options] bundle. These are passed
 * on to [Context.startActivity].
 *
 * For reuse in custom [AndroidScreenStarter] implementations, you can call [startWith] on instances
 * of this.
 */
@Parcelize
public data class IntentScreen(val intent: Intent, val options: Bundle? = null) : AndroidScreen {
  public fun startWith(context: Context): Boolean {
    context.startActivity(intent, options)
    return true
  }
}

@Stable
private class AndroidScreenAwareNavigator(
  private val delegate: Navigator,
  private val starter: AndroidScreenStarter,
) : Navigator by delegate {
  override fun goTo(screen: Screen): Boolean {
    return when (screen) {
      is AndroidScreen -> starter.start(screen)
      else -> delegate.goTo(screen)
    }
  }
}

/**
 * A custom starter for [AndroidScreen]s.
 *
 * Implementers should handle starting [AndroidScreen] subtypes, including [IntentScreen] and any
 * user-defined subtypes.
 */
public fun interface AndroidScreenStarter {
  /** Starts the given [screen]. */
  public fun start(screen: AndroidScreen): Boolean
}

/**
 * Returns a custom [Navigator] that can navigate to standard Android components like Activities.
 *
 * Note that this overload only handles [IntentScreen] and starting activities. More complex use
 * cases should implement a custom [AndroidScreenStarter].
 */
@CheckResult
@Composable
public fun rememberAndroidScreenAwareNavigator(delegate: Navigator, context: Context): Navigator {
  val starter =
    remember(context) {
      AndroidScreenStarter { screen ->
        when (screen) {
          is IntentScreen -> screen.startWith(context)
          else -> false
        }
      }
    }
  return rememberAndroidScreenAwareNavigator(delegate, starter)
}

/**
 * Returns a custom [Navigator] that can navigate to standard Android components like Activities.
 *
 * Note that this overload only handles [IntentScreen] and starting activities. More complex use
 * cases should implement a custom [AndroidScreenStarter].
 */
@CheckResult
@Composable
public fun rememberAndroidScreenAwareNavigator(
  delegate: Navigator,
  starter: AndroidScreenStarter,
): Navigator = remember(delegate) { AndroidScreenAwareNavigator(delegate, starter) }
