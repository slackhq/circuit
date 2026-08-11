// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization

import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.overwriteWith
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

/** Lets polymorphic [Screen] and [PopResult] properties reuse [CircuitSaveable] registrations. */
@OptIn(ExperimentalSerializationApi::class)
internal fun SerializersModule.withCircuitSaveableFallbacks(): SerializersModule {
  val source = this
  val bridge = SerializersModule {
    polymorphicDefaultSerializer(Screen::class) { value ->
      source.getPolymorphic(Screen::class, value)
        ?: source.getPolymorphic(CircuitSaveable::class, value)
    }
    polymorphicDefaultDeserializer(Screen::class) { className ->
      source.getPolymorphic(Screen::class, className)
        ?: source.circuitSaveableDeserializer(className, "Screen") { it as? Screen }
    }
    polymorphicDefaultSerializer(PopResult::class) { value ->
      source.getPolymorphic(PopResult::class, value)
        ?: source.getPolymorphic(CircuitSaveable::class, value)
    }
    polymorphicDefaultDeserializer(PopResult::class) { className ->
      source.getPolymorphic(PopResult::class, className)
        ?: source.circuitSaveableDeserializer(className, "PopResult") { it as? PopResult }
    }
  }
  return source overwriteWith bridge
}

@OptIn(ExperimentalSerializationApi::class)
private fun <T : CircuitSaveable> SerializersModule.circuitSaveableDeserializer(
  className: String?,
  expectedType: String,
  cast: (CircuitSaveable) -> T?,
): DeserializationStrategy<T>? {
  val delegate: DeserializationStrategy<CircuitSaveable> =
    getPolymorphic(CircuitSaveable::class, className) ?: return null
  return object : DeserializationStrategy<T> {
    override val descriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): T {
      val value = delegate.deserialize(decoder)
      return cast(value)
        ?: throw SerializationException(
          "Serializer for '$className' decoded a value that is not a $expectedType."
        )
    }
  }
}
