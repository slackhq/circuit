// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.coil.test

import coil3.Image
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.annotation.ExperimentalCoilApi
import coil3.test.FakeImage
import coil3.test.FakeImageLoaderEngine
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource
import org.junit.rules.ExternalResource

/**
 * A simple test rule around coil that installs a fake [factory] for tests and
 * [resets][SingletonImageLoader.reset] Coil after running.
 *
 * @property factory A [SingletonImageLoader.Factory] to use for image loading. Defaults to a simple
 *   fake instance.
 */
@ExperimentalCoilApi
class CoilRule(private val factory: SingletonImageLoader.Factory = defaultFactory()) :
  ExternalResource() {
  override fun before() {
    SingletonImageLoader.setSafe(factory)
  }

  @OptIn(DelicateCoilApi::class)
  override fun after() {
    SingletonImageLoader.reset()
  }

  companion object {
    /** A custom invoke that just uses a custom [image] to default in a [FakeImageLoaderEngine]. */
    operator fun invoke(image: Image) = CoilRule(factory = defaultFactory(image))

    /**
     * A custom invoke that just uses a custom [imageResourcePath] to default in a
     * [FakeImageLoaderEngine].
     */
    // TODO reading a path from another project doesn't appear to be supported
    @OptIn(ExperimentalResourceApi::class)
    operator fun invoke(imageResourcePath: String): CoilRule {
      val bytes = runBlocking { resource(imageResourcePath).readBytes() }
      return CoilRule(factory = defaultFactory(createImageFromBytes(bytes)))
    }
  }
}

private fun defaultFactory(image: Image = FakeImage()): SingletonImageLoader.Factory =
  SingletonImageLoader.Factory { context ->
    ImageLoader.Builder(context)
      .components { add(FakeImageLoaderEngine.Builder().default(image).build()) }
      .build()
  }

internal expect fun createImageFromBytes(bytes: ByteArray): Image
