// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import okio.fakefilesystem.FakeFileSystem
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// TODO eventually move to commonTest
@RunWith(RobolectricTestRunner::class)
class TokenStorageTest {
  @Test
  fun basicStore() = runTest {
    val tokenStorage =
      TokenStorageImpl(
        TokenStorageModule.provideDatastoreStorage(FakeStarAppDirs(FakeFileSystem()))
      )
    assertThat(tokenStorage.getAuthData()).isNull()
    val inputData =
      AuthenticationData("Bearer", Instant.fromEpochMilliseconds(0) + 1000.seconds, "token")
    tokenStorage.updateAuthData(inputData)
    assertThat(tokenStorage.getAuthData()).isEqualTo(inputData)
  }
}
