package com.slack.circuit.star.data

import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.Preferences
import com.slack.circuit.star.datastore.createStorage
import com.slack.circuit.star.di.AppScope
import dev.zacsweers.lattice.ContributesTo
import dev.zacsweers.lattice.Provides
import dev.zacsweers.lattice.SingleIn

@ContributesTo(AppScope::class)
interface TokenStorageModule {
  companion object {
    private const val TOKEN_STORAGE_FILE_NAME = "TokenManager"

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
}