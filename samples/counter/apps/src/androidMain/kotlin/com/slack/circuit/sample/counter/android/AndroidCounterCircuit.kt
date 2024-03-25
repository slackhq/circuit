// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter.android

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.slack.circuit.sample.counter.Counter
import com.slack.circuit.sample.counter.CounterScreen
import kotlinx.parcelize.Parcelize

// TODO why doesn't Parcelable get inherited through CounterScreen?
@Parcelize data object AndroidCounterScreen : CounterScreen(), Parcelable

@Preview(showBackground = true)
@Composable
private fun CounterPreview() {
  Counter(CounterScreen.State(0))
}

@Preview(showBackground = true)
@Composable
private fun CounterPreviewNegative() {
  Counter(CounterScreen.State(-1))
}
