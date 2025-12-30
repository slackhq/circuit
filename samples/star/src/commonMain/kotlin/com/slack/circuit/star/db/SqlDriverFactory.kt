// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.db

import androidx.compose.runtime.Stable
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

@Stable
fun interface SqlDriverFactory {
  suspend fun create(schema: SqlSchema<QueryResult.AsyncValue<Unit>>, name: String): SqlDriver
}
