package com.slack.circuit.star.db

import androidx.compose.runtime.Stable
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult.AsyncValue
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Stable
@Inject
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class NativeSqlDriverFactory : SqlDriverFactory {
  override suspend fun create(schema: SqlSchema<AsyncValue<Unit>>, name: String): SqlDriver {
    return NativeSqliteDriver(schema.synchronous(), name)
  }
}