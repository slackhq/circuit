package com.slack.circuit.star.data

import android.content.Context
import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.slack.circuit.star.datastore.createStorage
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.di.ApplicationContext
import com.squareup.anvil.annotations.ContributesTo
import dagger.Provides
import okio.Path.Companion.toOkioPath

@ContributesTo(AppScope::class)
object TokenStorageModule {
  private const val TOKEN_STORAGE_FILE_NAME = "TokenManager"

  // TODO android only
  @Provides
  fun provideDatastoreStorage(@ApplicationContext context: Context): Storage<Preferences> {
    return createStorage { context.preferencesDataStoreFile(TOKEN_STORAGE_FILE_NAME).toOkioPath() }
  }
}