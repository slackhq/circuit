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
package com.slack.circuit.sample.data

import com.slack.circuit.sample.di.AppScope
import com.slack.circuit.sample.di.SingleIn
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

@ContributesTo(AppScope::class)
@Module
object DataModule {
  @Provides
  @SingleIn(AppScope::class)
  fun provideMoshi(): Moshi {
    return Moshi.Builder().build()
  }

  @Provides
  @SingleIn(AppScope::class)
  fun provideOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
      .addInterceptor(
        HttpLoggingInterceptor().apply {
          level = HttpLoggingInterceptor.Level.BASIC
          redactHeader("Authorization")
        }
      )
      .build()
  }

  @Provides
  @SingleIn(AppScope::class)
  fun providePetfinderApi(moshi: Moshi, okHttpClient: OkHttpClient): PetfinderApi {
    val baseRetrofit =
      Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .baseUrl("https://api.petfinder.com/v2/")
        .client(okHttpClient)
        .build()
    val authApi = baseRetrofit.create<PetfinderAuthApi>()
    val tokenManager = TokenManager(authApi)
    val authInterceptor = AuthInterceptor(tokenManager)
    return baseRetrofit
      .newBuilder()
      .client(okHttpClient.newBuilder().addInterceptor(authInterceptor).build())
      .build()
      .create()
  }
}
