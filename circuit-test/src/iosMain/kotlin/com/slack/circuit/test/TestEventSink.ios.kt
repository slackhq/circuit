// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.test

public actual sealed interface BaseTestEventSinkType<UiEvent> : (UiEvent) -> Unit {
  actual override fun invoke(event: UiEvent)
}
