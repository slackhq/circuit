// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import android.os.Parcelable
import androidx.compose.runtime.Immutable

@Immutable public actual interface Screen : Parcelable

@Immutable
public actual interface NavigableScreen : Screen {
  public actual val route: String
}
