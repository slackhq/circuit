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

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TokenStorageTest {
  @Test
  fun basicStore() = runTest {
    val tokenStorage = TokenStorageImpl(ApplicationProvider.getApplicationContext())
    assertThat(tokenStorage.getAuthData()).isNull()
    val inputData = AuthenticationData("Bearer", Instant.EPOCH.plusSeconds(1000), "token")
    tokenStorage.updateAuthData(inputData)
    assertThat(tokenStorage.getAuthData()).isEqualTo(inputData)
  }
}
