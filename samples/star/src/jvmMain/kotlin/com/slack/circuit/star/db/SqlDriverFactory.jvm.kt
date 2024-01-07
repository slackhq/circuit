// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.db

import app.cash.sqldelight.db.QueryResult.Value
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import javax.inject.Inject

actual class SqlDriverFactory @Inject constructor() {
  actual fun create(schema: SqlSchema<Value<Unit>>, name: String): SqlDriver {
    // TODO what's the right way to do this on Desktop?
    //    val dbPath = createTempDirectory(name)
    //    return JdbcSqliteDriver(url = "jdbc:sqlite:${dbPath.absolutePathString()}")
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    schema.create(driver)
    return driver
  }
}
