// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.interop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rxjava3.subscribeAsState
import com.slack.circuit.Presenter
import com.slack.circuit.presenterOf
import com.slack.circuit.sample.counter.CounterEvent
import com.slack.circuit.sample.counter.CounterState
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject

/** An RxJava presenter that exposes an [Observable] of count changes. */
class RxCounterPresenter {
  private val count = BehaviorSubject.createDefault(0)

  fun increment() {
    count.onNext(count.value!! + 1)
  }

  fun decrement() {
    count.onNext(count.value!! - 1)
  }

  fun countObservable(): Observable<Int> = count.hide()
}

fun RxCounterPresenter.asCircuitPresenter(): Presenter<CounterState> {
  return presenterOf {
    val count by countObservable().subscribeAsState(initial = 0)
    CounterState(count) { event ->
      when (event) {
        is CounterEvent.Increment -> increment()
        is CounterEvent.Decrement -> decrement()
      }
    }
  }
}
