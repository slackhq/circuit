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
  val breed: String? = null,
  /** Life stage: "Puppy", "Young", "Adult", "Senior" */
  val age: String? = null,
  val sex: String? = null,
  /** Size, e.g. "Small 25 lbs (11 kg) or less" */
  val size: String? = null,
  val url: String,
  /** All photos with metadata (dimensions, aspect ratio, URL) */
  val photos: List<PhotoMetadata> = emptyList(),
  /** Plain text description (HTML stripped) */
  val description: String? = null,
  /** Original HTML description */
  val descriptionHtml: String? = null,
  /** Markdown description (converted from HTML) */
  val descriptionMarkdown: String? = null,
  @SerialName("short_description")
  val shortDescription: String? = null,
  /** Physical color */
  val color: String? = null,
  /** Compatibility and status attributes */
  val attributes: List<Attribute> = emptyList(),
)

/** A named attribute with display name. Only true attributes are included. */
@Serializable data class Attribute(val key: String, val display: String)

/**
 * Photo metadata with original dimensions.
 *
 * This includes the original (untransformed) image URL and its dimensions, allowing clients to
 * properly size and crop images based on their aspect ratio.
 */
@Serializable
data class PhotoMetadata(
  /** Original full-resolution image URL (from Cloudinary, no custom transformations) */
  val originalUrl: String,
  /** Width of original image in pixels */
  val width: Int,
  /** Height of original image in pixels */
  val height: Int,
  /** Aspect ratio of the original image (width / height) */
  val aspectRatio: Float,
)
