package com.slack.circuit.star.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.uikit.InterfaceOrientation
import androidx.compose.ui.uikit.LocalInterfaceOrientation

actual object Platform {
  @Composable
  actual fun ReportDrawnWhen(predicate: () -> Boolean) {
    // Nothing available on iOS
  }

  @OptIn(InternalComposeUiApi::class)
  @Composable
  actual fun isLandscape(): Boolean {
    return LocalInterfaceOrientation.current == InterfaceOrientation.Portrait ||
      LocalInterfaceOrientation.current == InterfaceOrientation.PortraitUpsideDown
  }

  actual val type: PlatformType = PlatformType.IOS
}
