package com.slack.circuit.star.db

import android.content.Context
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.slack.circuit.star.di.ApplicationContext
import javax.inject.Inject

// TODO expect/actual this class per platform
class SqlDriverFactory @Inject constructor(@ApplicationContext private val context: Context) {
  fun create(schema: SqlSchema<QueryResult.Value<Unit>>, name: String): SqlDriver {
    return AndroidSqliteDriver(schema, context, name)
  }
}