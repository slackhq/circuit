// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.kotlininject

import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Inject

class CircularDependencyTest {
  @Singleton
  @Component
  interface CircularComponent {
    val foo: Foo
  }

  @Singleton
  @Inject
  class Foo(val barProvider: () -> Bar) {
    fun print(message: String) {
      val bar = barProvider()
      check(bar.foo === this)
      bar.print(message)
    }
  }

  @Inject
  class Bar(val foo: Foo) {
    fun print(message: String) {
      println(message)
    }
  }
}
