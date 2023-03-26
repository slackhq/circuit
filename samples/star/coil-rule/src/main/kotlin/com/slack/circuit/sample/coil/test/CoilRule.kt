// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.coil.test

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.test.platform.app.InstrumentationRegistry
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.test.FakeImageLoaderEngine
import org.junit.rules.ExternalResource

/**
 * A simple test rule around coil that installs a fake [engine] for tests and [resets][Coil.reset]
 * Coil after running.
 */
@OptIn(ExperimentalCoilApi::class)
class CoilRule(
  private val engine: FakeImageLoaderEngine =
    FakeImageLoaderEngine.Builder().default(ColorDrawable(Color.LTGRAY)).build(),
  private val context: Context = defaultTestContext(),
) : ExternalResource() {
  override fun before() {
    val imageLoader = ImageLoader.Builder(context).components { add(engine) }.build()
    Coil.setImageLoader(imageLoader)
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
      context: Context = defaultTestContext(),
    ) = CoilRule(FakeImageLoaderEngine.Builder().default(drawable).build(), context)

    /**
     * A custom invoke that just uses a custom Drawable [id] to default in a
     * [FakeImageLoaderEngine].
     */
    operator fun invoke(
      @DrawableRes id: Int,
      context: Context = defaultTestContext(),
    ) = CoilRule(context.getDrawable(id)!!, context)
  }
}

private fun defaultTestContext(): Context =
  InstrumentationRegistry.getInstrumentation().targetContext
