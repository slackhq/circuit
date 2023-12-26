package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onPlaced
import com.slack.circuit.backstack.NavDecoration
import kotlinx.collections.immutable.ImmutableList

public class KeyNavigationDecoration(
  private val decoration: NavDecoration = NavigatorDefaults.DefaultDecoration,
  private val onBackInvoked: () -> Unit,
) : NavDecoration {

  @Composable
  override fun <T> DecoratedContent(
    args: ImmutableList<T>,
    backStackDepth: Int,
    modifier: Modifier,
    content: @Composable (T) -> Unit
  ) {
    decoration.DecoratedContent(
      args = args,
      backStackDepth = backStackDepth,
      modifier = modifier.focusOnPlacement().onEscKey(onBackInvoked),
      content = content,
    )
  }
}

@Composable
private fun Modifier.focusOnPlacement(): Modifier {
  val focusRequester = remember { FocusRequester() }
  return focusRequester(focusRequester).onPlaced { focusRequester.requestFocus() }
}

@Composable
private fun Modifier.onEscKey(action: () -> Unit): Modifier = onPreviewKeyEvent {
  if (it.type == KeyEventType.KeyUp && it.key == Key.Escape) {
    action()
    true
  } else {
    false
  }
}
