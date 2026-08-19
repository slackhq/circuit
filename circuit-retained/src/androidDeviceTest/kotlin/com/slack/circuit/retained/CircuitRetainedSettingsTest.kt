// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import com.google.common.truth.Truth.assertThat
import org.junit.Test

@OptIn(ExperimentalCircuitRetainedApi::class)
class CircuitRetainedSettingsTest {
  @Test
  fun firstPartyBackingIsEnabledByDefault() {
    assertThat(useFirstPartyByDefault).isTrue()
    assertThat(CircuitRetainedSettings.useFirstParty).isTrue()
  }

  @Test
  fun retainObserverCompatibilityRemainsOptIn() {
    assertThat(CircuitRetainedSettings.enforceRetainObserverCompatibility).isFalse()
  }
}
