// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.db

enum class Gender(val displayName: String) {
  MALE("Male"),
  FEMALE("Female"),
  UNKNOWN("Unknown");

  companion object {
    fun fromApiString(sex: String): Gender {
      return when (sex.lowercase()) {
        "male" -> MALE
        "female" -> FEMALE
        else -> UNKNOWN
      }
    }
  }
}
