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
 * A listener for tracking the state changes of a given [Screen]. This can be used for
 * instrumentation and other telemetry.
 */
public interface EventListener {

  /** Called when there is a new [state] returned by the [Presenter]. */
  public fun onState(state: Any) {}

  public fun interface Factory {
    public fun create(screen: Screen): EventListener
  }

  public companion object {
    public val NONE: EventListener = object : EventListener {}
  }
}
