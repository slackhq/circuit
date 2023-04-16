// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.db

import app.cash.sqldelight.ColumnAdapter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

class ImmutableListAdapter(private val delimiter: String) :
  ColumnAdapter<ImmutableList<String>, String> {
  override fun decode(databaseValue: String) =
    if (databaseValue.isEmpty()) {
      persistentListOf()
    } else {
      databaseValue.split(delimiter).toImmutableList()
    }
  override fun encode(value: ImmutableList<String>) = value.joinToString(separator = delimiter)
}
