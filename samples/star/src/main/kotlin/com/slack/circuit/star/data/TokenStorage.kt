// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.di.ApplicationContext
import com.slack.circuit.star.di.SingleIn
import com.squareup.anvil.annotations.ContributesBinding
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * A simple [TokenStorage] that uses `DataStore` to store [AuthenticationResponse] for reuse across
 * app sessions.
 */
interface TokenStorage {
  /** Updates the current stored auth data. */
  suspend fun updateAuthData(authData: AuthenticationData)
  /** Returns the current auth data or null if none are stored. */
  suspend fun getAuthData(): AuthenticationData?
}

data class AuthenticationData(val tokenType: String, val expiration: Instant, val token: String)

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class TokenStorageImpl @Inject constructor(@ApplicationContext context: Context) : TokenStorage {
  private val datastore =
    PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(TOKEN_STORAGE_FILE_NAME) }

  override suspend fun updateAuthData(authData: AuthenticationData) {
    datastore.edit { prefs ->
      prefs[expirationKey] = authData.expiration.toEpochMilli()
      prefs[authTokenTypeKey] = authData.tokenType
      prefs[authTokenKey] = authData.token
    }
  }

  override suspend fun getAuthData(): AuthenticationData? {
    val expiration = datastore.data.first()[expirationKey]
    val tokenType = datastore.data.first()[authTokenTypeKey]
    val token = datastore.data.first()[authTokenKey]
    return if (expiration != null && tokenType != null && token != null) {
      AuthenticationData(tokenType, Instant.ofEpochMilli(expiration), token)
    } else {
      null
    }
  }

  companion object {
    private const val TOKEN_STORAGE_FILE_NAME = "TokenManager"
    val authTokenKey = stringPreferencesKey("AUTH_TOKEN")
    val expirationKey = longPreferencesKey("AUTH_TOKEN_EXPIRATION")
    val authTokenTypeKey = stringPreferencesKey("AUTH_TOKEN_TYPE")
  }
}
