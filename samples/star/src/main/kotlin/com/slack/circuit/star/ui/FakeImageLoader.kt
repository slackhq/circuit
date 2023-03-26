// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.VisibleForTesting
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
@VisibleForTesting(otherwise = VisibleForTesting.NONE)
class FakeImageLoader(private val drawable: Drawable = ColorDrawable(Color.LTGRAY)) : ImageLoader {

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
