// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data.petfinder

import com.slack.circuit.star.data.AuthenticationData
import com.slack.circuit.star.data.TokenStorage
import kotlin.time.Clock.System
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationResponse(
  @SerialName("token_type") val tokenType: String,
  @SerialName("expires_in") val expiresIn: Long,
  @SerialName("access_token") val accessToken: String,
)

suspend fun TokenStorage.updateAuthData(response: AuthenticationResponse) {
  updateAuthData(
    AuthenticationData(
      response.tokenType,
      System.now().plus(response.expiresIn.seconds),
      response.accessToken,
    )
  )
}
