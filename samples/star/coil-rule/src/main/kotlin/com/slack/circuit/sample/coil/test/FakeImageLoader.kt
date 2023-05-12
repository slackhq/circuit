// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.coil.test

import coil.ComponentRegistry
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.test.FakeImageLoaderEngine
import kotlinx.coroutines.CompletableDeferred

/**
 * Fake Coil ImageLoader based on example found here:
 * https://coil-kt.github.io/coil/image_loaders/#testing
 *
 * This delegates everything to a backing [FakeImageLoaderEngine] that intercepts all calls.
 */
@OptIn(ExperimentalCoilApi::class)
internal class FakeImageLoader(engine: FakeImageLoaderEngine) : ImageLoader {

  override val defaults = DefaultRequestOptions()
  override val components = ComponentRegistry.Builder().add(engine).build()
  override val memoryCache: MemoryCache?
    get() = null

  override val diskCache: DiskCache?
    get() = null

  override fun enqueue(request: ImageRequest): Disposable {
    return object : Disposable {
      override val job = CompletableDeferred<ImageResult>()
      override val isDisposed
        get() = true

      override fun dispose() = Unit
    }
  }

  override suspend fun execute(request: ImageRequest): ImageResult =
    throw AssertionError("This should never be called.")

  override fun newBuilder() = throw AssertionError("This should never be called.")

  override fun shutdown() = Unit
}
