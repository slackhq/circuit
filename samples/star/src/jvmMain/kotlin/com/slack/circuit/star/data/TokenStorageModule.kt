// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.Preferences
import com.slack.circuit.star.datastore.createStorage
import com.slack.circuit.star.di.AppScope
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.optional.SingleIn
import dagger.Module
import dagger.Provides

// TODO better reconcile this with the android version
@ContributesTo(AppScope::class)
@Module
object TokenStorageModule {
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
