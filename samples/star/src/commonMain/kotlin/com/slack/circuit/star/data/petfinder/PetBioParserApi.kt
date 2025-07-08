// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data.petfinder

import com.slack.circuit.star.petdetail.PetBioParser
import com.slack.eithernet.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

interface PetBioParserApi {
  suspend fun parseBio(url: String): ApiResult<String, Unit>
}

class PetBioParserApiImpl(private val httpClient: HttpClient) : PetBioParserApi {
  override suspend fun parseBio(url: String) =
    try {
      val bio =
        withContext(Dispatchers.IO) {
          val response = httpClient.get(url)
          val html = response.bodyAsText()
          PetBioParser.parse(html)
        }
      ApiResult.success(bio)
    } catch (e: Exception) {
      e.asKtorApiResult()
    }
}
