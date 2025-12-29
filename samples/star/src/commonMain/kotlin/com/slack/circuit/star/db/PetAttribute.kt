// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.db

import app.cash.sqldelight.ColumnAdapter

/**
 * A pet attribute with a key and display name. Only true/positive attributes are stored (e.g.,
 * "good_with_cats" -> "Good with cats").
 */
data class PetAttribute(val key: String, val display: String)

/**
 * Photo with metadata for storage in the database. Contains the URL, dimensions, and aspect ratio.
 */
data class Photo(val originalUrl: String, val width: Int, val height: Int, val aspectRatio: Float)

/** Adapter for storing a list of [PetAttribute] in SQLDelight. */
class PetAttributeListAdapter : ColumnAdapter<List<PetAttribute>, String> {
  override fun decode(databaseValue: String): List<PetAttribute> {
    if (databaseValue.isEmpty()) return emptyList()
    return databaseValue.split("|").mapNotNull { entry ->
      val parts = entry.split(":", limit = 2)
      if (parts.size == 2) {
        PetAttribute(key = parts[0], display = parts[1])
      } else {
        null
      }
    }
  }

  override fun encode(value: List<PetAttribute>): String {
    return value.joinToString("|") { "${it.key}:${it.display}" }
  }
}

/**
 * Adapter for storing a list of [Photo] in SQLDelight. Format:
 * "url;;width;;height;;aspectRatio|url;;width;;height;;aspectRatio|..."
 *
 * Uses ";;" as field delimiter since URLs can contain commas (e.g., Cloudinary's "f_auto,q_auto").
 */
class PhotoListAdapter : ColumnAdapter<List<Photo>, String> {
  override fun decode(databaseValue: String): List<Photo> {
    if (databaseValue.isEmpty()) return emptyList()
    return databaseValue.split("|").mapNotNull { entry ->
      val parts = entry.split(";;", limit = 4)
      if (parts.size == 4) {
        Photo(
          originalUrl = parts[0],
          width = parts[1].toIntOrNull() ?: return@mapNotNull null,
          height = parts[2].toIntOrNull() ?: return@mapNotNull null,
          aspectRatio = parts[3].toFloatOrNull() ?: return@mapNotNull null,
        )
      } else {
        null
      }
    }
  }

  override fun encode(value: List<Photo>): String {
    return value.joinToString("|") {
      "${it.originalUrl};;${it.width};;${it.height};;${it.aspectRatio}"
    }
  }
}
