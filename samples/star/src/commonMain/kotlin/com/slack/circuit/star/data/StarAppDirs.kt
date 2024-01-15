// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import okio.FileSystem
import okio.Path

/** Common interface for access to different directories on the filesystem. */
interface StarAppDirs {
  val fs: FileSystem
  val userConfig: Path
  val userData: Path
  val userCache: Path
}
