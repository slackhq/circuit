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

@Keep
actual interface PetfinderAuthApi {
  // "grant_type=client_credentials&client_id=$API_KEY&client_secret=$SECRET"
  @FormUrlEncoded
  @POST("oauth2/token")
  actual suspend fun authenticate(
    @Field("client_id") clientId: String,
    @Field("client_secret") secret: String,
    @Field("grant_type") grantType: String,
  ): ApiResult<AuthenticationResponse, Unit>
}

@Keep
actual interface PetfinderApi {
  @GET("animals")
  actual suspend fun animals(
    @Query("sort") sort: String,
    @Query("limit") limit: Int,
    @Query("page") page: Int,
    @Query("organization") organization: String,
  ): ApiResult<AnimalsResponse, Unit>

  @GET("animals/{id}") actual suspend fun animal(@Path("id") id: Long): ApiResult<Animal, Unit>

  @JSoupEndpoint @GET("") actual suspend fun animalBio(@Url url: String): ApiResult<String, Unit>
}
