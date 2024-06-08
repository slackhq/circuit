// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.star.data.petfinder.AuthenticationResponse
import com.slack.circuit.star.data.petfinder.PetfinderAuthApi
import com.slack.circuit.star.data.petfinder.updateAuthData
import com.slack.eithernet.ApiResult
import com.slack.eithernet.test.enqueue
import com.slack.eithernet.test.newEitherNetController
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TokenManagerTest {
  private val apiController = newEitherNetController<PetfinderAuthApi>()
  private val authApi = apiController.api

  @Test
  fun happyPath() = runTest {
    val tokenStorage = FakeTokenStorage()
    val tokenManager = TokenManager(authApi, tokenStorage)
    val current = tokenManager.last()
    assertThat(current).isNull()
    apiController.enqueue(
      PetfinderAuthApi::authenticate,
      ApiResult.success(AuthenticationResponse("Bearer", 1000, "token")),
    )
    tokenManager.refreshToken()
    val tokens = tokenManager.last()
    assertThat(tokens!!.accessToken).isEqualTo("token")
  }

  @Test
  fun initialData() = runTest {
    val tokenStorage =
      FakeTokenStorage().apply { updateAuthData(AuthenticationResponse("Bearer", 1000, "token")) }
    val tokenManager = TokenManager(authApi, tokenStorage)
    val current = tokenManager.last()
    assertNotNull(current)
    assertThat(current.accessToken).isEqualTo("token")
  }
}

private class FakeTokenStorage : TokenStorage {
  var authData: AuthenticationData? = null

  override suspend fun updateAuthData(authData: AuthenticationData) {
    this.authData = authData
  }

  override suspend fun getAuthData(): AuthenticationData? = authData
}
