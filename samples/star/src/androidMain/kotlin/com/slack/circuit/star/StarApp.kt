// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.slack.circuit.star.di.AppComponent

class StarApp : Application(), SingletonImageLoader.Factory {

  private val appComponent by lazy { AppComponent.create(this) }

  fun appComponent() = appComponent

  override fun newImageLoader(context: PlatformContext): ImageLoader = appComponent.imageLoader
}
