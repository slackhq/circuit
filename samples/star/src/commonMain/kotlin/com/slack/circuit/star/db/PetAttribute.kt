// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.db

import app.cash.sqldelight.ColumnAdapter

/**
 * A pet attribute with a key and display name. Only true/positive attributes are stored
 * (e.g., "good_with_cats" -> "Good with cats").
 */
data class PetAttribute(val key: String, val display: String)

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
