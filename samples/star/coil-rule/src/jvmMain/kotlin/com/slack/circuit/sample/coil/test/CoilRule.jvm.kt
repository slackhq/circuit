// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.coil.test

import coil3.Image
import coil3.asCoilImage
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image as SkiaImage

actual fun createImageFromBytes(bytes: ByteArray): Image {
  val skiaImage = SkiaImage.makeFromEncoded(bytes)
  return Bitmap.makeFromImage(skiaImage).asCoilImage()
}
