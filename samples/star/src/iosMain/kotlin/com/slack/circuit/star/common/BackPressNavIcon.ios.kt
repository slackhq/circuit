package com.slack.circuit.star.common

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.LocalUIViewController
import platform.UIKit.UINavigationController
import platform.UIKit.navigationController

@Composable
actual fun BackPressNavIcon(
  modifier: Modifier,
  onClick: (() -> Unit)?,
  iconButtonContent: @Composable () -> Unit,
) {
  val uiViewController = LocalUIViewController.current
  val finalOnClick = remember {
    onClick
      ?: {
        val navigationController = uiViewController.navigationController
        if (navigationController?.canGoBack() == true) {
          navigationController.popViewControllerAnimated(true)
        }
      }
  }
  IconButton(modifier = modifier, onClick = finalOnClick) { iconButtonContent() }
}

private fun UINavigationController.canGoBack(): Boolean {
  return viewControllers.size > 1
}
