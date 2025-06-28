package com.slack.circuit.star.data

import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.Preferences
import com.slack.circuit.star.datastore.createStorage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

private const val TOKEN_STORAGE_FILE_NAME = "TokenManager"

@ContributesTo(AppScope::class)
interface TokenStorageProviders {
  @SingleIn(AppScope::class)
  @Provides
  fun provideDatastoreStorage(appDirs: StarAppDirs): Storage<Preferences> {
    return createStorage(appDirs.fs) {
      val dir = appDirs.userConfig / "token_storage"
      createDirectory(dir)
      dir.resolve("$TOKEN_STORAGE_FILE_NAME.preferences_pb")
    }
  }
}
