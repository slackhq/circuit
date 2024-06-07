// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data.petfinder

import androidx.annotation.Keep
import com.slack.eithernet.ApiResult

private const val API_KEY = "SYWzK0sDRY8UMNjHM1IC3WDM8qLO6vCjCSfwf6aY5QlISE65kJ"
private const val SECRET = "aA4VMgoPSReox5Ve67cNHI9M06JEgh68fjkG46Ee"

@Keep
expect interface PetfinderAuthApi {
  // "grant_type=client_credentials&client_id=$API_KEY&client_secret=$SECRET"
  suspend fun authenticate(
    clientId: String = API_KEY,
    secret: String = SECRET,
    grantType: String = "client_credentials",
  ): ApiResult<AuthenticationResponse, Unit>
}

@Keep
expect interface PetfinderApi {
  suspend fun animals(
    sort: String = "-recent", // recent, -recent, distance, -distance,
    limit: Int = 100, // (default: 20, max: 100)
    page: Int = 1,
    organization: String = "NY835",
  ): ApiResult<AnimalsResponse, Unit>

  suspend fun animal(id: Long): ApiResult<Animal, Unit>

  suspend fun animalBio(url: String): ApiResult<String, Unit>
}
