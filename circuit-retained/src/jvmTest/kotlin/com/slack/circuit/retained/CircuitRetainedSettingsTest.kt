// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import com.google.common.truth.Truth.assertThat
import org.junit.Test

@OptIn(ExperimentalCircuitRetainedApi::class)
class CircuitRetainedSettingsTest {
  @Test
  fun firstPartyBackingRemainsOptIn() {
    assertThat(useFirstPartyByDefault).isFalse()
    assertThat(CircuitRetainedSettings.useFirstParty).isFalse()
  }

  @Test
  fun retainObserverCompatibilityRemainsOptIn() {
    assertThat(CircuitRetainedSettings.enforceRetainObserverCompatibility).isFalse()
  }
}
