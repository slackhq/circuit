// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.overlays

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties

@OptIn(ExperimentalMaterial3Api::class)
internal actual fun createBottomSheetProperties(
  shouldDismissOnBackPress: Boolean
): ModalBottomSheetProperties {
  return ModalBottomSheetProperties(DEFAULT_PROPERTIES.securePolicy, shouldDismissOnBackPress)
}
