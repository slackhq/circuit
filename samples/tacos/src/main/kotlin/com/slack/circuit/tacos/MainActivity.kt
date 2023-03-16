// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitConfig
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.tacos.repository.IngredientsRepositoryImpl
import com.slack.circuit.tacos.step.FillingsProducerImpl
import com.slack.circuit.tacos.step.ToppingsProducerImpl
import com.slack.circuit.tacos.step.confirmationProducer
import com.slack.circuit.tacos.step.summaryProducer
import com.slack.circuit.tacos.ui.theme.TacoTheme

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val circuitConfig: CircuitConfig = CircuitConfig
      .Builder()
      .addPresenterFactory(buildPresenterFactory())
      .addUiFactory(OrderTacosUiFactory())
      .build()

    setContent {
      TacoTheme {
        rememberSystemUiController().run {
          setStatusBarColor(MaterialTheme.colorScheme.background)
        }

        CircuitCompositionLocals(circuitConfig) {
          CircuitContent(OrderTacosScreen)
        }
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
    confirmationProducer = { details, _ -> confirmationProducer(details) },
    summaryProducer = { sink -> summaryProducer(sink) }
  )
}