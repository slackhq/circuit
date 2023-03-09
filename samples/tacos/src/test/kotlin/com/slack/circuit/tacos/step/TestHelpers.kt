package com.slack.circuit.tacos.step

import com.slack.circuit.tacos.model.Ingredient
import kotlinx.collections.immutable.persistentListOf

internal inline fun <reified T : Any> OrderStep.State.asInstanceOf(): T =
  (this as? T) ?: error("unable to cast $this to ${T::class.simpleName}")
