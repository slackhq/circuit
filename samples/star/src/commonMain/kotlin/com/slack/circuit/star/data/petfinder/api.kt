// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data.petfinder

import com.slack.eithernet.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.parameters

private const val API_KEY = "SYWzK0sDRY8UMNjHM1IC3WDM8qLO6vCjCSfwf6aY5QlISE65kJ"
private const val SECRET = "aA4VMgoPSReox5Ve67cNHI9M06JEgh68fjkG46Ee"

internal const val PET_FINDER_API_URL = "https://api.petfinder.com/v2/"

interface PetfinderAuthApi {
  suspend fun authenticate(
    clientId: String = API_KEY,
    secret: String = SECRET,
    grantType: String = "client_credentials",
  ): ApiResult<AuthenticationResponse, Unit>
}

class PetfinderAuthApiImpl(private val httpClient: HttpClient) : PetfinderAuthApi {
  override suspend fun authenticate(clientId: String, secret: String, grantType: String) =
    httpClient.apiResultOf<AuthenticationResponse, Unit> {
      httpClient.submitForm(
        "${PET_FINDER_API_URL}oauth2/token",
        formParameters =
          parameters {
            append("grant_type", grantType)
            append("client_id", clientId)
            append("client_secret", secret)
          },
      )
    }
}

interface PetfinderApi {
  suspend fun animals(
    sort: String = "-recent", // recent, -recent, distance, -distance,
    limit: Int = 100, // (default: 20, max: 100)
    page: Int = 1,
    organization: String = "NY835",
  ): ApiResult<AnimalsResponse, Unit>

  suspend fun animal(id: Long): ApiResult<Animal, Unit>
}

class PetfinderApiImpl(private val httpClient: HttpClient) : PetfinderApi {
  override suspend fun animals(sort: String, limit: Int, page: Int, organization: String) =
    httpClient.apiResultOf<AnimalsResponse, Unit> {
      httpClient.get("${PET_FINDER_API_URL}animals") {
        parameter("sort", sort)
        parameter("limit", limit)
        parameter("page", page)
        parameter("organization", organization)
      }
    }

  override suspend fun animal(id: Long) =
    httpClient.apiResultOf<Animal, Unit> { httpClient.get("${PET_FINDER_API_URL}animals/$id") }
}
