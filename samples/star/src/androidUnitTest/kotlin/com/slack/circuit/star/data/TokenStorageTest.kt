// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TokenStorageTest {
  @Test
  fun basicStore() = runTest {
    val tokenStorage =
      TokenStorageImpl(
        TokenStorageModule.provideDatastoreStorage(ApplicationProvider.getApplicationContext())
      )
    assertThat(tokenStorage.getAuthData()).isNull()
    val inputData =
      AuthenticationData("Bearer", Instant.fromEpochMilliseconds(0) + 1000.seconds, "token")
    tokenStorage.updateAuthData(inputData)
    assertThat(tokenStorage.getAuthData()).isEqualTo(inputData)
  }
}
