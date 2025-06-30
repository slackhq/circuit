// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import coil3.ImageLoader
import com.slack.circuit.foundation.Circuit

interface CommonAppGraph {
  val circuit: Circuit
  val imageLoader: ImageLoader
}
