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
package com.slack.circuit.sample.interop

import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.Presenter
import com.slack.circuit.presenterOf
import com.slack.circuit.sample.counter.CounterEvent
import com.slack.circuit.sample.counter.CounterState

/** A simple presenter that uses a listener for signaling count changes. */
class SimpleCounterPresenter {

  private var onCountChangedListener: OnCountChangedListener? = null

  private var count = 0

  fun increment() {
    count++
    notifyCountChanged()
  }

  fun decrement() {
    count--
    notifyCountChanged()
  }

  fun setOnCountChangedListener(listener: OnCountChangedListener?) {
    onCountChangedListener = listener
  }

  private fun notifyCountChanged() {
    onCountChangedListener?.onCountChanged(count)
  }

  fun interface OnCountChangedListener {
    fun onCountChanged(count: Int)
  }
}

fun SimpleCounterPresenter.asCircuitPresenter(): Presenter<CounterState> {
  return presenterOf {
    var count by remember { mutableStateOf(0) }
    remember {
      object : RememberObserver {
        val listener =
          SimpleCounterPresenter.OnCountChangedListener { newCount -> count = newCount }
        override fun onAbandoned() {
          setOnCountChangedListener(null)
        }

        override fun onForgotten() {
          setOnCountChangedListener(null)
        }

        override fun onRemembered() {
          setOnCountChangedListener(listener)
        }
      }
    }
    CounterState(count) { event ->
      when (event) {
        is CounterEvent.Increment -> increment()
        is CounterEvent.Decrement -> decrement()
      }
    }
  }
}
