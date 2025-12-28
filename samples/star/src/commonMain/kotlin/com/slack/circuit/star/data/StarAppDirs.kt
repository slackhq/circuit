// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/** Common interface for access to different directories on the filesystem. */
interface StarAppDirs {
  val fs: FileSystem
  val userConfig: Path
  val userData: Path
  val userCache: Path

  /** Deletes all app data directories. */
  fun clearAll() {
    fs.deleteRecursively(userConfig, mustExist = false)
    fs.deleteRecursively(userData, mustExist = false)
    fs.deleteRecursively(userCache, mustExist = false)
  }
}

class FakeStarAppDirs(override val fs: FileSystem) : StarAppDirs {
  override val userConfig: Path = "/userConfig".toPath().also(fs::createDirectories)
  override val userData: Path = "/userData".toPath().also(fs::createDirectories)
  override val userCache: Path = "/userCache".toPath().also(fs::createDirectories)
}
