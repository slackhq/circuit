/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.slack.circuit.backstack.SaveableBackStack
import java.util.UUID

/** A basic navigation interface for navigating between [screens][Screen]. */
@Stable
interface Navigator {
  fun goTo(screen: Screen)

  fun pop(): Screen?

  object NoOp : Navigator {
    override fun goTo(screen: Screen) {}
    override fun pop(): Screen? = null
  }
}

/**
 * Returns a new [Navigator] for navigating within [CircuitContents][CircuitContent].
 *
 * @see NavigableCircuitContent
 *
 * @param backstack The backing [SaveableBackStack] to navigate.
 * @param onRootPop The callback to handle root [Navigator.pop] calls.
 */
@Composable
fun rememberCircuitNavigator(backstack: SaveableBackStack, onRootPop: (() -> Unit)?): Navigator {
  val continuity = viewModel<Continuity>()
  val backstackKey = rememberSaveable { UUID.randomUUID().toString() }
  val navImpl = remember { continuity.navigatorForBackStack(backstackKey, ::NavigatorImpl) }
  navImpl.bind(backstack, onRootPop)
  val activity = LocalContext.current.findActivity()
  remember(backstack) {
    object : RememberObserver {
      override fun onAbandoned() {
        navImpl.unbind()
        disposeIfNotChangingConfiguration()
      }

      override fun onForgotten() {
        navImpl.unbind()
        disposeIfNotChangingConfiguration()
      }

      override fun onRemembered() {
        // Do nothing
      }

      fun disposeIfNotChangingConfiguration() {
        if (activity?.isChangingConfigurations != true) {
          continuity.removeNavigatorForBackStack(backstackKey)
        }
      }
    }
  }

  return navImpl
}

internal class NavigatorImpl : Navigator {

  private var _backstack: SaveableBackStack? = null
  private var onRootPop: (() -> Unit)? = null
  private val backstack: SaveableBackStack
    get() = checkNotNull(_backstack) { "Navigator is null" }

  // TODO these are called on the Applier thread from compose
  fun bind(backstack: SaveableBackStack, onRootPop: (() -> Unit)?) {
    check(_backstack == null) { "Navigator has already been bound" }
    _backstack = backstack
    this.onRootPop = onRootPop
  }

  fun unbind() {
    _backstack = null
    onRootPop = null
  }

  override fun goTo(screen: Screen) {
    backstack.push(screen)
  }

  override fun pop(): Screen? {
    backstack.pop()?.screen?.let {
      return it
    }
    onRootPop?.invoke()
    return null
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as NavigatorImpl

    if (backstack != other.backstack) return false
    if (onRootPop != other.onRootPop) return false

    return true
  }

  override fun hashCode(): Int {
    var result = backstack.hashCode()
    result = 31 * result + (onRootPop?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String {
    return "NavigatorImpl(backstack=$backstack, onRootPop=$onRootPop)"
  }
}

/** Calls [Navigator.pop] until the given [predicate] is matched or it pops the root. */
fun Navigator.popUntil(predicate: (Screen) -> Boolean) {
  while (true) {
    val screen = pop() ?: break
    if (predicate(screen)) {
      break
    }
  }
}
