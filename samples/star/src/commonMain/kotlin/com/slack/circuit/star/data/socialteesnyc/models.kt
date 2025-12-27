// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data.socialteesnyc

import kotlin.time.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class PetsListResponse(
  val pets: List<Animal>,
  @Serializable(with = Iso8601InstantSerializer::class) val updatedAt: Instant,
)

private object Iso8601InstantSerializer : KSerializer<Instant> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("Iso8601Instant", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Instant) {
    encoder.encodeString(value.toString())
  }

  override fun deserialize(decoder: Decoder): Instant {
    return Instant.parse(decoder.decodeString())
  }
}

@Serializable
data class Animal(
  val id: String,
  val name: String,
  /** "Dog" or "Cat" */
  @SerialName("type") val petType: String,
  val breed: String,
  /** Life stage: "Puppy", "Young", "Adult", "Senior" */
  val age: String,
  val sex: String,
  /** Size with weight, e.g. "Small 25 lbs (11 kg) or less", or null */
  val size: String? = null,
  val url: String,
  val photoUrl: String? = null,
  val description: String,
  @SerialName("short_description") val shortDescription: String,
)
