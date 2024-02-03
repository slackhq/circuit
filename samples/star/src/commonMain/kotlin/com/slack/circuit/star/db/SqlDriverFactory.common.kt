// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

expect class SqlDriverFactory {
  fun create(schema: SqlSchema<QueryResult.Value<Unit>>, name: String): SqlDriver
}
