// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.db

import android.content.Context
import app.cash.sqldelight.db.QueryResult.Value
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.slack.circuit.star.di.ApplicationContext
import javax.inject.Inject

actual class SqlDriverFactory
@Inject
constructor(@ApplicationContext private val context: Context) {
  actual fun create(schema: SqlSchema<Value<Unit>>, name: String): SqlDriver {
    return AndroidSqliteDriver(schema, context, name)
  }
}
