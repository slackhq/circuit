// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import com.slack.circuit.star.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import net.harawata.appdirs.AppDirsFactory
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DesktopStarAppDirs(override val fs: FileSystem) : StarAppDirs {

  private val appDirs = AppDirsFactory.getInstance()

  override val userConfig: Path by lazy {
    appDirs.getUserConfigDir(APP_NAME, APP_VERSION, APP_AUTHOR).toPath().also(fs::createDirectories)
  }

  override val userData: Path by lazy {
    appDirs.getUserDataDir(APP_NAME, APP_VERSION, APP_AUTHOR).toPath().also(fs::createDirectories)
  }

  override val userCache: Path by lazy {
    appDirs.getUserCacheDir(APP_NAME, APP_VERSION, APP_AUTHOR).toPath().also(fs::createDirectories)
  }

  private companion object {
    const val APP_NAME = "STAR"
    const val APP_VERSION = "1.0.0"
    const val APP_AUTHOR = "slackhq"
  }
}
