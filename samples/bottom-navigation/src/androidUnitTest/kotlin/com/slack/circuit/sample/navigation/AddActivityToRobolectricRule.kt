// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.robolectric.Shadows

// https://github.com/robolectric/robolectric/pull/4736
class AddActivityToRobolectricRule : TestWatcher() {
  override fun starting(description: Description?) {
    super.starting(description)
    val appContext: Application = ApplicationProvider.getApplicationContext()
    Shadows.shadowOf(appContext.packageManager)
      .addActivityIfNotPresent(
        ComponentName(appContext.packageName, ComponentActivity::class.java.name)
      )
  }
}
