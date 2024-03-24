// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.interop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.presenter.presenterOf

/** A simple presenter that uses a listener for signaling count changes. */
class SimpleCounterPresenter(initialCount: Int) {

  private var onCountChangedListener: OnCountChangedListener? = null

  internal var count = initialCount

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
  // TODO why is this key necessary? The returned presenter is a new instance
  val key = this
  return presenterOf {
    var count by remember(key) { mutableIntStateOf(this@asCircuitPresenter.count) }
    val listener =
      remember(key) {
        SimpleCounterPresenter.OnCountChangedListener { newCount -> count = newCount }
      }
    DisposableEffect(listener) {
      setOnCountChangedListener(listener)
      onDispose { setOnCountChangedListener(null) }
    }
    CounterScreen.State(count) { event ->
      when (event) {
        is CounterScreen.Event.Increment -> increment()
        is CounterScreen.Event.Decrement -> decrement()
      }
    }
  }
}
