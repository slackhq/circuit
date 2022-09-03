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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.ViewModel

internal fun Context.findActivity(): Activity? {
  var context = this
  while (context is ContextWrapper) {
    if (context is Activity) return context
    context = context.baseContext
  }
  return null
}

class Continuity : ViewModel() {
  private val presenters = mutableMapOf<Screen, Presenter<*, *>?>()

  @Suppress("UNCHECKED_CAST")
  fun <T : Any, E : Any> presenterForScreen(
    screen: Screen,
    defaultValue: () -> Presenter<T, E>?
  ): Presenter<T, E>? {
    println("presenterForScreen $screen")
    return presenters.getOrPut(screen, defaultValue) as Presenter<T, E>?
  }

  @Suppress("UNCHECKED_CAST")
  fun <T : Any, E : Any> removePresenterForScreen(screen: Screen): Presenter<T, E>? {
    println("Removing presenter for screen $screen")
    return presenters.remove(screen) as Presenter<T, E>?
  }

  override fun onCleared() {
    presenters.clear()
  }
}
