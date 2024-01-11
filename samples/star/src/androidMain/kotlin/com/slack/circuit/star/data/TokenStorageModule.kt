// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import android.content.Context
import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.slack.circuit.star.datastore.createStorage
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.di.ApplicationContext
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.optional.SingleIn
import dagger.Module
import dagger.Provides
import okio.FileSystem
import okio.Path.Companion.toOkioPath

@ContributesTo(AppScope::class)
@Module
object TokenStorageModule {
  private const val TOKEN_STORAGE_FILE_NAME = "TokenManager"

  @SingleIn(AppScope::class)
  @Provides
  fun provideDatastoreStorage(@ApplicationContext context: Context): Storage<Preferences> {
    return createStorage(FileSystem.SYSTEM) {
      context.preferencesDataStoreFile(TOKEN_STORAGE_FILE_NAME).toOkioPath()
    }
  }
}
