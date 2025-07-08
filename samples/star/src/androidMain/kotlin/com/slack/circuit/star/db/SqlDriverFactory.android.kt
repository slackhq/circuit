// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.db

import android.content.Context
import androidx.compose.runtime.Stable
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.QueryResult.Value
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.slack.circuit.star.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Stable
@Inject
@ContributesBinding(AppScope::class)
class AndroidSqlDriverFactory(@ApplicationContext private val context: Context) : SqlDriverFactory {
  override suspend fun create(schema: SqlSchema<QueryResult.AsyncValue<Unit>>, name: String) =
    AndroidSqliteDriver(schema.synchronous(), context, name)
}
