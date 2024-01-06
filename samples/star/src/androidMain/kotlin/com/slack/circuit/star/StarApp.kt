// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import android.app.Application
import com.slack.circuit.star.di.AppComponent

class StarApp : Application() {

  private val appComponent by lazy { AppComponent.create(this) }

  fun appComponent() = appComponent
}
