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
  private var count = BehaviorSubject.createDefault(0)

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
