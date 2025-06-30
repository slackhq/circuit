// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.slack.circuit.star.di.AppGraph
import dev.zacsweers.metro.createGraphFactory

class StarApp : Application(), SingletonImageLoader.Factory {

  val appGraph by lazy { createGraphFactory<AppGraph.Factory>().create(this) }

  override fun newImageLoader(context: PlatformContext): ImageLoader = appGraph.imageLoader
}
