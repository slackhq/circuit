// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.coil.test

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.annotation.DrawableRes
import androidx.test.platform.app.InstrumentationRegistry
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.test.FakeImageLoaderEngine
import org.junit.rules.ExternalResource

/**
 * A simple test rule around coil that installs a fake [engineProvider] for tests and
 * [resets][Coil.reset] Coil after running.
 *
 * @property engineProvider A function that returns a [FakeImageLoaderEngine] to use for image
 *   loading.
 * @property contextProvider A function that returns a [Context] to use for image loading. If none
 *   is provided, a limited fake implementation will be used.
 */
@OptIn(ExperimentalCoilApi::class)
class CoilRule(
  private val engineProvider: () -> FakeImageLoaderEngine = {
    FakeImageLoaderEngine.Builder().default(ColorDrawable(Color.LTGRAY).wrap()).build()
  },
  private val contextProvider: (() -> Context)? = null,
) : ExternalResource() {
  override fun before() {
    // We must defer this lookup as late as possible as Android APIs aren't available until
    // runtime in Paparazzi tests.
    Coil.setImageLoader {
      contextProvider?.let {
        ImageLoader.Builder(it()).components { add(engineProvider()) }.build()
      }
        ?: FakeImageLoader(engineProvider())
    }
  }

  override fun after() {
    Coil.reset()
  }

  companion object {
    /**
     * A custom invoke that just uses a custom [drawable] to default in a [FakeImageLoaderEngine].
     */
    operator fun invoke(
      drawable: Drawable,
      contextProvider: (() -> Context)? = null,
    ) =
      CoilRule(
        engineProvider = {
          FakeImageLoaderEngine.Builder().default(drawable.wrapIfNecessary()).build()
        },
        contextProvider = contextProvider
      )

    /**
     * A custom invoke that just uses a custom Drawable [id] to default in a
     * [FakeImageLoaderEngine].
     */
    operator fun invoke(
      @DrawableRes id: Int,
      context: Context = defaultTestContext(),
    ) = CoilRule(context.getDrawable(id)!!) { context }
  }
}

private fun Drawable.wrapIfNecessary(): Drawable {
  return when (this) {
    // Wrap the color drawable so it isn't automatically converted into a ColorPainter.
    is ColorDrawable -> wrap()
    else -> this
  }
}

private fun ColorDrawable.wrap(): Drawable {
  return object : LayerDrawable(arrayOf(this)) {
    override fun getIntrinsicWidth() = 100
    override fun getIntrinsicHeight() = 100
  }
}

private fun defaultTestContext(): Context =
  InstrumentationRegistry.getInstrumentation().targetContext
