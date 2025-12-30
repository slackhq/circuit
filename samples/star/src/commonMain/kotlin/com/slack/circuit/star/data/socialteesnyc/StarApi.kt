// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data.socialteesnyc

import com.slack.eithernet.ApiResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okio.IOException

private const val PETS_URL =
  "https://raw.githubusercontent.com/ZacSweers/socialteesjs/refs/heads/main/data/pets.json"

/** Toggle to use fake API data from bundled JSON instead of network. */
const val USE_FAKE_API = false

interface StarApi {
  suspend fun getPets(): ApiResult<PetsListResponse, Unit>
}

/** Delegate that picks between [RealStarApi] and [FakeStarApi] based on [USE_FAKE_API]. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class StarApiImpl(realApi: Lazy<RealStarApi>, fakeApi: Lazy<FakeStarApi>) :
  StarApi by if (USE_FAKE_API) fakeApi.value else realApi.value

@Inject
@SingleIn(AppScope::class)
class RealStarApi(httpClient: HttpClient, json: Json) : StarApi {
  private val httpClient =
    httpClient.config {
      // We're reading from raw github, which serves in plain text responses
      install(ContentNegotiation) { json(json, ContentType.Text.Plain) }
    }

  override suspend fun getPets(): ApiResult<PetsListResponse, Unit> {
    return try {
      val response = httpClient.get(PETS_URL)
      ApiResult.success(response.body<PetsListResponse>())
    } catch (e: ClientRequestException) {
      ApiResult.httpFailure(e.response.status.value)
    } catch (e: ServerResponseException) {
      ApiResult.httpFailure(e.response.status.value)
    } catch (e: IOException) {
      ApiResult.networkFailure(e)
    } catch (e: Exception) {
      ApiResult.unknownFailure(e)
    }
  }
}
