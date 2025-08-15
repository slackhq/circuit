// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.internal.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
actual fun rememberViewModel(input: String?): MutableIntState {
  return viewModel<TestStateViewModel>(key = input, factory = TestStateViewModel.Factory)
    .counterState
}

private class TestStateViewModel : ViewModel() {
  val counterState = mutableIntStateOf(0)

  object Factory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return TestStateViewModel() as T
    }
  }
}
