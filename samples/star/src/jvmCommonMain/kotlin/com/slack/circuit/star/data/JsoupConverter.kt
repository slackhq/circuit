// Copyright (C) 2019 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import androidx.annotation.Keep
import java.io.IOException
import java.lang.reflect.Type
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit

@Keep annotation class JSoupEndpoint

/** A [Converter] that only decodes responses with a given [convertBody]. */
class JsoupConverter<T> private constructor(private val convertBody: (ResponseBody) -> T) :
  Converter<ResponseBody, T> {

  @Throws(IOException::class)
  override fun convert(value: ResponseBody): T {
    return convertBody(value)
  }

  companion object {
    /** Creates a new factory for creating converter. We only care about decoding responses. */
    fun <T> newFactory(decoder: (ResponseBody) -> T): Converter.Factory {
      return object : Converter.Factory() {
        private val converter by lazy { JsoupConverter(decoder) }

        override fun responseBodyConverter(
          type: Type,
          annotations: Array<Annotation>,
          retrofit: Retrofit
        ): Converter<ResponseBody, *>? {
          return if (annotations.any { it is JSoupEndpoint }) {
            converter
          } else {
            null
          }
        }
      }
    }
  }
}
