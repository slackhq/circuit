// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data.petfinder

import androidx.annotation.Keep
import com.slack.circuit.star.data.JSoupEndpoint
import com.slack.eithernet.ApiResult
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

private const val API_KEY = "SYWzK0sDRY8UMNjHM1IC3WDM8qLO6vCjCSfwf6aY5QlISE65kJ"
private const val SECRET = "aA4VMgoPSReox5Ve67cNHI9M06JEgh68fjkG46Ee"

@Keep
interface PetfinderAuthApi {
  // "grant_type=client_credentials&client_id=$API_KEY&client_secret=$SECRET"
  @FormUrlEncoded
  @POST("oauth2/token")
  suspend fun authenticate(
    @Field("client_id") clientId: String = API_KEY,
    @Field("client_secret") secret: String = SECRET,
    @Field("grant_type") grantType: String = "client_credentials",
  ): ApiResult<AuthenticationResponse, Unit>
}

@Keep
interface PetfinderApi {
  @GET("animals")
  suspend fun animals(
    @Query("sort") sort: String = "-recent", // recent, -recent, distance, -distance,
    @Query("limit") limit: Int = 100, // (default: 20, max: 100)
    @Query("page") page: Int = 1,
    @Query("organization") organization: String = "NY835",
  ): ApiResult<AnimalsResponse, Unit>

  @GET("animals/{id}") suspend fun animal(@Path("id") id: Long): ApiResult<Animal, Unit>

  @JSoupEndpoint @GET("") suspend fun animalBio(@Url url: String): ApiResult<String, Unit>
}
