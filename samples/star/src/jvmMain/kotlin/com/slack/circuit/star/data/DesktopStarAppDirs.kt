// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import com.slack.circuit.star.di.AppScope
import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.optional.SingleIn
import javax.inject.Inject
import net.harawata.appdirs.AppDirsFactory
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DesktopStarAppDirs @Inject constructor(override val fs: FileSystem) : StarAppDirs {

  private val appDirs = AppDirsFactory.getInstance()

  override val userConfig: Path by lazy {
    appDirs.getUserConfigDir(APP_NAME, APP_VERSION, APP_AUTHOR).toPath().also {
      fs.createDirectories(it)
    }
  }

  override val userData: Path by lazy {
    appDirs.getUserDataDir(APP_NAME, APP_VERSION, APP_AUTHOR).toPath().also {
      fs.createDirectories(it)
    }
  }

  override val userCache: Path by lazy {
    appDirs.getUserCacheDir(APP_NAME, APP_VERSION, APP_AUTHOR).toPath().also {
      fs.createDirectories(it)
    }
  }

  private companion object {
    const val APP_NAME = "STAR"
    const val APP_VERSION = "1.0.0"
    const val APP_AUTHOR = "slackhq"
  }
}
