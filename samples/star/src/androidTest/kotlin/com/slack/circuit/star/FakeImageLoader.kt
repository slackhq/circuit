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
package com.slack.circuit.star

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import coil.ComponentRegistry
import coil.ImageLoader
import coil.decode.DataSource
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import kotlinx.coroutines.CompletableDeferred

/**
 * Fake Coil ImageLoader based on example found here:
 * https://coil-kt.github.io/coil/image_loaders/#testing
 */
class FakeImageLoader(
  context: Context,
  @DrawableRes drawableResId: Int,
) : ImageLoader {
  private val drawable by lazy {
    context.getDrawable(drawableResId) ?: throw IllegalArgumentException("drawable not found!")
  }

  override val defaults = DefaultRequestOptions()
  override val components = ComponentRegistry()
  override val memoryCache: MemoryCache?
    get() = null
  override val diskCache: DiskCache?
    get() = null

  override fun enqueue(request: ImageRequest): Disposable {
    request.target?.onStart(request.placeholder)
    request.target?.onSuccess(drawable)

    return object : Disposable {
      override val job = CompletableDeferred(newResult(request, drawable))
      override val isDisposed
        get() = true
      override fun dispose() = Unit
    }
  }

  override suspend fun execute(request: ImageRequest): ImageResult = newResult(request, drawable)

  private fun newResult(request: ImageRequest, drawable: Drawable): SuccessResult {
    return SuccessResult(
      drawable = drawable,
      request = request,
      dataSource = DataSource.MEMORY_CACHE
    )
  }

  override fun newBuilder() = throw UnsupportedOperationException()

  override fun shutdown() = Unit
}
