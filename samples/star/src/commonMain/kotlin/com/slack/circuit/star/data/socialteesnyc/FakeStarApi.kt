// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data.socialteesnyc

import com.slack.circuit.star.resources.Res
import com.slack.eithernet.ApiResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * A fake [StarApi] implementation that reads from a bundled JSON file.
 *
 * Useful for testing and local development without network access.
 */
@Inject
@SingleIn(AppScope::class)
class FakeStarApi(private val json: Json) : StarApi {
  @OptIn(ExperimentalResourceApi::class)
  override suspend fun getPets(): ApiResult<PetsListResponse, Unit> {
    return try {
      val bytes = Res.readBytes("files/pets.json")
      val jsonString = bytes.decodeToString()
      val response = json.decodeFromString<PetsListResponse>(jsonString)
      ApiResult.success(response)
    } catch (e: Exception) {
      ApiResult.unknownFailure(e)
    }
  }
}
