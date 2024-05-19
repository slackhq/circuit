// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.screen

import android.os.Parcelable
import androidx.compose.runtime.Immutable

@Immutable public actual interface Screen : Parcelable
@Immutable public actual interface StaticScreen : Screen
