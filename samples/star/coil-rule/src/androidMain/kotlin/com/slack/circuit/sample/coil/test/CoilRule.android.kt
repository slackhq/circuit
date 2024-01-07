// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.coil.test

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.annotation.DrawableRes
import androidx.test.platform.app.InstrumentationRegistry
import coil3.asCoilImage

/** A custom invoke that just uses a custom [drawable] to default in a [FakeImageLoader]. */
operator fun CoilRule.Companion.invoke(
  drawable: Drawable,
) = invoke(image = wrapIfNecessary(drawable).asCoilImage())

/** A custom invoke that just uses a custom Drawable [id] to default in a [FakeImageLoader]. */
operator fun CoilRule.Companion.invoke(
  @DrawableRes id: Int,
  context: Context = defaultTestContext(),
) = CoilRule(context.getDrawable(id)!!)

private fun defaultTestContext(): Context {
  return InstrumentationRegistry.getInstrumentation().targetContext
}

// Prevent optimizing drawables into compose primitives
// https://github.com/coil-kt/coil/blob/deb887cee703551a280685baa58facb159668751/coil-compose-base/src/main/java/coil/compose/AsyncImagePainter.kt#L338
private fun wrapIfNecessary(drawable: Drawable): Drawable {
  return when (drawable) {
    is ColorDrawable -> wrapInLayer(drawable)
    else -> drawable
  }
}

private fun wrapInLayer(drawable: Drawable): Drawable {
  return object : LayerDrawable(arrayOf(drawable)) {
    override fun getIntrinsicWidth() = drawable.intrinsicWidth

    override fun getIntrinsicHeight() = drawable.intrinsicHeight
  }
}
