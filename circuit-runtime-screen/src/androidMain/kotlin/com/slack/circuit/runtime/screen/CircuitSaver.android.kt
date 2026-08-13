// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.screen

import android.os.Parcelable

public actual val DefaultCircuitSaver: CircuitSaver = AndroidDefaultCircuitSaver

private object AndroidDefaultCircuitSaver : CircuitSaver() {
  override fun save(value: CircuitSaveable): Any? = value.takeIf { it is Parcelable }

  override fun restore(saved: Any): CircuitSaveable? = saved as? CircuitSaveable
}
