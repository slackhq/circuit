package com.slack.circuit.tacos.step

internal inline fun <reified T : Any> OrderStep.State.asInstanceOf(): T =
  (this as? T) ?: error("unable to cast $this to ${T::class.simpleName}")
