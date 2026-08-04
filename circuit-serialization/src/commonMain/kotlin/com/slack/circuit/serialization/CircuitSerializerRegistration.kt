// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization

import com.slack.circuit.runtime.screen.CircuitSaveable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/** Registers a serializable Circuit screen or pop result for polymorphic serialization. */
public fun interface CircuitSerializerRegistration {
  /** Adds this registration to [builder]. */
  public fun register(builder: PolymorphicModuleBuilder<CircuitSaveable>)
}

/** Creates a serializers module containing [registrations]. */
public fun circuitSerializersModule(
  registrations: Iterable<CircuitSerializerRegistration>
): SerializersModule = SerializersModule {
  polymorphic(CircuitSaveable::class) {
    for (registration in registrations) {
      registration.register(this)
    }
  }
}
