// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.Preferences
import com.slack.circuit.star.datastore.createStorage
import com.slack.circuit.star.di.AppScope
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import kotlin.io.path.createTempDirectory
import okio.Path.Companion.toOkioPath

// TODO better reconcile this with the android version
@ContributesTo(AppScope::class)
@Module
object TokenStorageModule {
  private const val TOKEN_STORAGE_FILE_NAME = "TokenManager"

  @Provides
  fun provideDatastoreStorage(): Storage<Preferences> {
    return createStorage {
      createTempDirectory("star-datastore")
        .resolve("$TOKEN_STORAGE_FILE_NAME.preferences_pb")
        .toOkioPath()
    }
  }
}
