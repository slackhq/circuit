// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.db

import androidx.compose.runtime.Stable
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.slack.circuit.star.data.StarAppDirs
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.io.path.absolutePathString

@Stable
@Inject
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class JvmSqlDriverFactory(private val appDirs: StarAppDirs) : SqlDriverFactory {
  override suspend fun create(
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
    name: String,
  ): SqlDriver {
    val driver =
      if (name.isEmpty()) {
          JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        } else {
          val dbPath = appDirs.userConfig.resolve("$name.db")
          JdbcSqliteDriver(url = "jdbc:sqlite:${dbPath.toNioPath().absolutePathString()}")
        }
        .also { schema.create(it).await() }
    return driver
  }
}
