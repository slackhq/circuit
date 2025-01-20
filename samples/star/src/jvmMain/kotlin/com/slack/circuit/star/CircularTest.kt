// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import dagger.Component
import javax.inject.Inject
import javax.inject.Singleton

class CircularDependencyTest {
  @Singleton
  @Component
  interface CircularComponent {
    val foo: Foo
  }

  @Singleton
  class Foo @Inject constructor(val barProvider: dagger.Lazy<Bar>) {
    fun print(message: String) {
      val bar = barProvider.get()
      check(bar.foo === this)
      bar.print(message)
    }
  }

  class Bar @Inject constructor(val foo: Foo) {
    fun print(message: String) {
      println(message)
    }
  }
}
