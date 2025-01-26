// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.db

import app.cash.sqldelight.db.QueryResult.Value
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.slack.circuit.star.data.StarAppDirs
import dev.zacsweers.lattice.Inject
import kotlin.io.path.absolutePathString

@Inject
actual class SqlDriverFactory(private val appDirs: StarAppDirs) {
  actual fun create(schema: SqlSchema<Value<Unit>>, name: String): SqlDriver {
    val dbPath = appDirs.userConfig.resolve("$name.db")
    val driver = JdbcSqliteDriver(url = "jdbc:sqlite:${dbPath.toNioPath().absolutePathString()}")
    //    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    schema.create(driver)
    return driver
  }
}
