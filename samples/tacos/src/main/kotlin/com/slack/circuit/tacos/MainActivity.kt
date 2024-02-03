// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.slack.circuit.tacos.repository.IngredientsRepositoryImpl
import com.slack.circuit.tacos.step.FillingsProducerImpl
import com.slack.circuit.tacos.step.ToppingsProducerImpl
import com.slack.circuit.tacos.step.confirmationProducer
import com.slack.circuit.tacos.step.summaryProducer
import com.slack.circuit.tacos.ui.theme.TacoTheme

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val circuit: Circuit =
      Circuit.Builder()
        .addPresenterFactory(buildPresenterFactory())
        .addUiFactory(buildUiFactory())
        .build()

    setContent {
      TacoTheme {
        rememberSystemUiController().setStatusBarColor(MaterialTheme.colorScheme.background)
        CircuitCompositionLocals(circuit) { CircuitContent(OrderTacosScreen) }
      }
    }
  }
}

private fun buildPresenterFactory(): Presenter.Factory =
  Presenter.Factory { _, _, _ ->
    OrderTacosPresenter(
      fillingsProducer = FillingsProducerImpl(IngredientsRepositoryImpl),
      toppingsProducer = ToppingsProducerImpl(IngredientsRepositoryImpl),
      confirmationProducer = { details, _ -> confirmationProducer(details) },
      summaryProducer = { _, sink -> summaryProducer(sink) },
    )
  }

private fun buildUiFactory(): Ui.Factory =
  Ui.Factory { _, _ ->
    ui<OrderTacosScreen.State> { state, modifier -> OrderTacosUi(state, modifier) }
  }
