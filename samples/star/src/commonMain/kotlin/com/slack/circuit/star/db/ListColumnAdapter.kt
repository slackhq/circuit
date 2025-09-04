// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.db

import app.cash.sqldelight.ColumnAdapter

class ListColumnAdapter(private val delimiter: String) : ColumnAdapter<List<String>, String> {
  override fun decode(databaseValue: String) =
    if (databaseValue.isEmpty()) {
      emptyList()
    } else {
      databaseValue.split(delimiter).toList()
    }

  override fun encode(value: List<String>) = value.joinToString(separator = delimiter)
}
