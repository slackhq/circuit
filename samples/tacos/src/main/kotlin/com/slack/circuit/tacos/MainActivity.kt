// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.slack.circuit.CircuitCompositionLocals
import com.slack.circuit.CircuitConfig
import com.slack.circuit.NavigableCircuitContent
import com.slack.circuit.Presenter
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.push
import com.slack.circuit.rememberCircuitNavigator
import com.slack.circuit.tacos.repository.IngredientsRepositoryImpl
import com.slack.circuit.tacos.step.FillingsProducerImpl
import com.slack.circuit.tacos.step.ToppingsProducerImpl
import com.slack.circuit.tacos.step.confirmationProducer

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val circuitConfig: CircuitConfig = CircuitConfig
      .Builder()
      .addPresenterFactory(buildPresenterFactory())
      .addUiFactory(OrderTacosUiFactory())
      .build()

    setContent {
      val backstack = rememberSaveableBackStack { push(OrderTacosScreen) }
      val navigator = rememberCircuitNavigator(backstack)
      CircuitCompositionLocals(circuitConfig) {
        NavigableCircuitContent(navigator, backstack)
      }
    }
  }
}

private fun buildPresenterFactory(): Presenter.Factory {
  val fillingsProducer = FillingsProducerImpl(IngredientsRepositoryImpl)
  val toppingsProducer = ToppingsProducerImpl(IngredientsRepositoryImpl)

  return OrderTacosPresenterFactory(
    fillingsProducer = fillingsProducer,
    toppingsProducer = toppingsProducer,
    confirmationProducer = { details, _ -> confirmationProducer(details) }
  )
}