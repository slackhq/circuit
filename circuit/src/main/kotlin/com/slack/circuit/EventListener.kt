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

/**
 * A listener for tracking the state changes and event emissions of a given [Screen]. This can be
 * used for instrumentation and other telemetry.
 */
interface EventListener {

  /** Called when there is a new [state] returned by the [Presenter]. */
  fun onState(state: Any) {}

  /** Called when there is a new [event] emitted by the [Ui]. */
  fun onEvent(event: Any) {}

  fun interface Factory {
    fun create(screen: Screen): EventListener
  }

  companion object {
    val NONE = object : EventListener {}
  }
}
