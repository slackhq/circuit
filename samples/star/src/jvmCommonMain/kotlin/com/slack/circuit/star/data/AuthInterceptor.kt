// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
  override fun intercept(chain: Chain): Response {
    // TODO check for 401s and re-auth?
    val request = runBlocking {
      val (name, value) = tokenManager.requestAuthHeader()
      chain.request().newBuilder().addHeader(name, value).build()
    }
    return chain.proceed(request)
  }
}
