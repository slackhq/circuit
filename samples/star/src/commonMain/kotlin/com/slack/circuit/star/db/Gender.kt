// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.db

enum class Gender(val displayName: String) {
  MALE("Male"),
  FEMALE("Female");

  companion object {
    fun fromApiString(gender: String?): Gender? {
      return when (gender?.lowercase()) {
        "male" -> MALE
        "female" -> FEMALE
        else -> null
      }
    }
  }
}
