// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import android.content.Context
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.android.AndroidScreen
import com.slack.circuitx.android.AndroidScreenStarter
import com.slack.circuitx.android.IntentScreen
import com.slack.circuitx.android.rememberAndroidScreenAwareNavigator

/** A [NavigationInterceptor] version of [rememberAndroidScreenAwareNavigator] */
public class AndroidScreenAwareNavigationInterceptor(private val starter: AndroidScreenStarter) :
  NavigationInterceptor {

  public constructor(
    context: Context
  ) : this(
    AndroidScreenStarter { screen ->
      when (screen) {
        is IntentScreen -> screen.startWith(context)
        else -> false
      }
    }
  )

  override fun goTo(screen: Screen): InterceptedGoToResult {
    return when (screen) {
      is AndroidScreen ->
        if (starter.start(screen)) {
          NavigationInterceptor.SuccessConsumed
        } else {
          InterceptedResult.Failure(consumed = true)
        }
      else -> NavigationInterceptor.Skipped
    }
  }
}
