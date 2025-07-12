// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Instant
import kotlinx.coroutines.flow.first

/**
 * A simple [TokenStorage] that uses `DataStore` to store `AuthenticationResponse` for reuse across
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
@Inject
class TokenStorageImpl(storage: Storage<Preferences>) : TokenStorage {
  private val datastore = PreferenceDataStoreFactory.create(storage = storage)

  override suspend fun updateAuthData(authData: AuthenticationData) {
    datastore.edit { prefs ->
      prefs[expirationKey] = authData.expiration.toEpochMilliseconds()
      prefs[authTokenTypeKey] = authData.tokenType
      prefs[authTokenKey] = authData.token
    }
  }

  override suspend fun getAuthData(): AuthenticationData? {
    val expiration = datastore.data.first()[expirationKey]
    val tokenType = datastore.data.first()[authTokenTypeKey]
    val token = datastore.data.first()[authTokenKey]
    return if (expiration != null && tokenType != null && token != null) {
      AuthenticationData(tokenType, Instant.fromEpochMilliseconds(expiration), token)
    } else {
      null
    }
  }

  companion object {
    val authTokenKey = stringPreferencesKey("AUTH_TOKEN")
    val expirationKey = longPreferencesKey("AUTH_TOKEN_EXPIRATION")
    val authTokenTypeKey = stringPreferencesKey("AUTH_TOKEN_TYPE")
  }
}
