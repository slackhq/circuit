// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.db

enum class Size {
  SMALL,
  MEDIUM,
  LARGE,
  UNKNOWN;

  companion object {
    /**
     * Parses a size string from the API like "Small 25 lbs (11 kg) or less" into a [Size] enum.
     * Returns [UNKNOWN] if the size cannot be determined.
     */
    fun fromApiString(size: String?): Size {
      if (size == null) return UNKNOWN
      val lowerSize = size.lowercase()
      return when {
        lowerSize.startsWith("small") -> SMALL
        lowerSize.startsWith("medium") -> MEDIUM
        lowerSize.startsWith("large") || lowerSize.startsWith("extra large") -> LARGE
        else -> UNKNOWN
      }
    }
  }
}
