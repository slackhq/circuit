// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.interop

import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.sample.counter.CounterScreen

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

fun SimpleCounterPresenter.asCircuitPresenter(): Presenter<CounterScreen.State> {
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
    CounterScreen.State(count) { event ->
      when (event) {
        is CounterScreen.Event.Increment -> increment()
        is CounterScreen.Event.Decrement -> decrement()
        else -> Unit
      }
    }
  }
}
