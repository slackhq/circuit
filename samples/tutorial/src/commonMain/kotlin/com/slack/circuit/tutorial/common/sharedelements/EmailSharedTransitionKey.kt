// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tutorial.common.sharedelements

import com.slack.circuit.sharedelements.SharedTransitionKey

data class EmailSharedTransitionKey(val id: String, val type: ElementType) : SharedTransitionKey {
  enum class ElementType {
    SenderImage,
    SenderName,
    Subject,
    Body,
  }
}
