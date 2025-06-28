package com.slack.circuit.star.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import okio.fakefilesystem.FakeFileSystem

class TokenStorageTest {
  @Test
  fun basicStore() = runTest {
    val tokenStorage =
      TokenStorageImpl(
        TokenStorageProviders.createDatastoreStorage(FakeStarAppDirs(FakeFileSystem()))
      )
    assertThat(tokenStorage.getAuthData()).isNull()
    val inputData =
      AuthenticationData(
        "Bearer",
        Instant.Companion.fromEpochMilliseconds(0) + 1000.seconds,
        "token",
      )
    tokenStorage.updateAuthData(inputData)
    assertThat(tokenStorage.getAuthData()).isEqualTo(inputData)
  }
}
