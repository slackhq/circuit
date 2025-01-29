// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import android.content.Context
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.di.ApplicationContext
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class ContextStarAppDirs(@ApplicationContext private val context: Context, override val fs: FileSystem) :
  StarAppDirs {

  override val userConfig: Path by lazy {
    (context.filesDir.toOkioPath() / "config").also(fs::createDirectories)
  }

  override val userData: Path by lazy {
    (context.filesDir.toOkioPath() / "data").also(fs::createDirectories)
  }

  override val userCache: Path by lazy { context.cacheDir.toOkioPath() }
}
