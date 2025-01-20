// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import org.junit.Test

class CircularTest {
  @Test
  fun test() {
    val component = DaggerCircularDependencyTest_CircularComponent.create()
    val foo: CircularDependencyTest.Foo = component.foo
    foo.print("Hello, world!")
  }
  @Test
  fun test2() {
    val component: CycleMultibindsGraph = DaggerCycleMultibindsGraph.create()
    val y = component.y()
  }
}
