// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import androidx.annotation.Keep
import com.slack.eithernet.ApiResult
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
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
    @Field("grant_type") grantType: String = "client_credentials"
  ): ApiResult<AuthenticationResponse, Unit>
}

@Keep
interface PetfinderApi {
  @GET("animals")
  suspend fun animals(
    @Query("sort") sort: String = "-recent", // recent, -recent, distance, -distance,
    @Query("limit") limit: Int = 100, // (default: 20, max: 100)
    @Query("page") page: Int = 1,
    @Query("organization") organization: String = "NY835"
  ): ApiResult<AnimalsResponse, Unit>

  @GET("animals/{id}") suspend fun animal(@Path("id") id: Long): ApiResult<Animal, Unit>

  @JSoupEndpoint
  @GET("") suspend fun animalBio(@Url url: String): ApiResult<String, Unit>
}

@JsonClass(generateAdapter = true)
data class AnimalsResponse(val animals: List<Animal>, val pagination: Pagination)

@JsonClass(generateAdapter = true)
data class Animal(
  val id: Long,
  @Json(name = "organization_id") val organizationId: String,
  val url: String,
  val type: String,
  val species: String,
  val gender: String,
  val breeds: Breeds,
  val colors: Colors,
  val age: String,
  val size: String,
  val coat: String? = null,
  val name: String,
  val description: String? = null,
  val photos: List<Photo>,
  val videos: List<Video>,
  val status: String,
  // "attributes": {
  //     "spayed_neutered": true,
  //     "house_trained": true,
  //     "declawed": false,
  //     "special_needs": false,
  //     "shots_current": true
  // },
  val attributes: Map<String, Boolean>,
  // "environment": {
  //     "children": false,
  //     "dogs": true,
  //     "cats": true
  // },
  val environment: Map<String, Boolean>,
  val tags: List<String>,
  // "contact": {
  //     "email": "petfindertechsupport@gmail.com",
  //     "phone": "555-555-5555",
  //     "address": {
  //         "address1": null,
  //         "address2": null,
  //         "city": "Jersey City",
  //         "state": "NJ",
  //         "postcode": "07097",
  //         "country": "US"
  //     }
  // },
  //  val contact: Contact,
  // "2018-09-04T14:49:09+0000"
  @Json(name = "published_at") val publishedAt: String,
  // 0.4095
  val distance: Double? = null, // TODO wtf are the units of this
  // "_links": {
  //     "self": {
  //         "href": "/v2/animals/124"
  //     },
  //     "type": {
  //         "href": "/v2/types/cat"
  //     },
  //     "organization": {
  //         "href": "/v2/organizations/nj333"
  //     }
  // }
  @Json(name = "_links") val links: Links,
)

@JsonClass(generateAdapter = true)
data class Links(val self: Link, val type: Link, val organization: Link)

@JsonClass(generateAdapter = true) data class Link(val href: String)

@JsonClass(generateAdapter = true)
data class Breeds(
  val primary: String? = null,
  val secondary: String? = null,
  val mixed: Boolean = false,
  val unknown: Boolean = false
)

@JsonClass(generateAdapter = true)
data class Colors(
  val primary: String? = null,
  val secondary: String? = null,
  val tertiary: String? = null
)

@JsonClass(generateAdapter = true)
data class Photo(val small: String, val medium: String, val large: String, val full: String)

@JsonClass(generateAdapter = true)
data class Video(
  // An iframe snippet to embed
  val embed: String
)

@JsonClass(generateAdapter = true)
data class Pagination(
  @Json(name = "count_per_page") val countPerPage: Int = 20,
  @Json(name = "total_count") val totalCount: Int = 320,
  @Json(name = "current_page") val currentPage: Int = 1,
  @Json(name = "total_pages") val totalPages: Int = 16,
)

@JsonClass(generateAdapter = true)
data class AuthenticationResponse(
  @Json(name = "token_type") val tokenType: String,
  @Json(name = "expires_in") val expiresIn: Long,
  @Json(name = "access_token") val accessToken: String,
)

suspend fun TokenStorage.updateAuthData(response: AuthenticationResponse) {
  updateAuthData(
    AuthenticationData(
      response.tokenType,
      Instant.now().plusSeconds(response.expiresIn),
      response.accessToken
    )
  )
}

/**
 * A hypothetical token manager that stores an auth token. Just in-memory and not thread-safe for
 * now.
 */
class TokenManager(private val api: PetfinderAuthApi, private val tokenStorage: TokenStorage) {
  private val mutex = Mutex()

  /** Authenticates a [request]. */
  suspend fun authenticate(request: Request): Request {
    return authenticate(request, false)
  }

  private suspend fun authenticate(request: Request, isAfterRefresh: Boolean): Request {
    println("INFO: Authenticating request ${request.url}")
    val newBuilder = request.newBuilder()
    val (tokenType, expiration, token) =
      tokenStorage.getAuthData()
        ?: run {
          refreshToken()
          return authenticate(request, isAfterRefresh)
        }
    if (Instant.now().isAfter(expiration)) {
      check(!isAfterRefresh)
      refreshToken()
      return authenticate(request, isAfterRefresh)
    } else {
      newBuilder.addHeader("Authorization", "$tokenType $token")
    }

    return newBuilder.build()
  }

  private suspend fun refreshToken() =
    mutex.withLock {
      println("INFO: Refreshing token")

      when (val result = api.authenticate()) {
        is ApiResult.Success -> tokenStorage.updateAuthData(result.value)
        is ApiResult.Failure -> {
          println("ERROR: Failed to refresh token: $result")
        }
      }
    }
}

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    // TODO check for 401s and re-auth?
    val request = runBlocking { tokenManager.authenticate(chain.request()) }
    return chain.proceed(request)
  }
}
