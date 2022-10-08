/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.first

interface TokenStorage {
  suspend fun updateToken(token: AuthenticationData)
  suspend fun getToken(): AuthenticationData?
}

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class TokenStorageImpl @Inject constructor(@ApplicationContext context: Context) : TokenStorage {
  private val datastore =
    PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("TokenManager") }

  override suspend fun updateToken(token: AuthenticationData) {
    datastore.edit { prefs ->
      prefs[expirationKey] = Instant.now().plus(Duration.ofSeconds(token.expiresIn)).toEpochMilli()
      prefs[authTokenTypeKey] = token.tokenType
      prefs[authTokenKey] = token.accessToken
    }
  }

  override suspend fun getToken(): AuthenticationData? {
    val expiration = datastore.data.first()[expirationKey]
    val tokenType = datastore.data.first()[authTokenTypeKey]
    val token = datastore.data.first()[authTokenKey]
    return if (expiration != null && tokenType != null && token != null) {
      AuthenticationData(tokenType, expiration, token)
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
