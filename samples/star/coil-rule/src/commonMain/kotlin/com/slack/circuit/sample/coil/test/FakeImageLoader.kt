// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.coil.test

import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.ImageRequest.Defaults
import coil3.request.ImageResult
import coil3.test.FakeImageLoaderEngine
import kotlinx.coroutines.CompletableDeferred

/**
 * Fake Coil ImageLoader based on example found here:
 * https://coil-kt.github.io/coil/image_loaders/#testing
 *
 * This delegates everything to a backing [FakeImageLoaderEngine] that intercepts all calls.
 */
internal class FakeImageLoader(engine: FakeImageLoaderEngine) : ImageLoader {

  override val defaults = Defaults.DEFAULT
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
