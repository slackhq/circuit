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
package com.slack.circuit.star.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slack.circuit.CircuitConfig
import com.slack.circuit.CircuitInject
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.ScreenUi
import com.slack.circuit.Ui
import com.slack.circuit.star.R
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.ui
import com.squareup.anvil.annotations.ContributesMultibinding
import javax.inject.Inject
import kotlinx.parcelize.Parcelize

@Parcelize
object AboutScreen : Screen {
  object State : CircuitUiState
}

@CircuitInject<AboutScreen>
class AboutPresenter : Presenter<AboutScreen.State> {
  @Composable override fun present() = AboutScreen.State
}

@ContributesMultibinding(AppScope::class)
class AboutUiFactory @Inject constructor() : Ui.Factory {
  override fun create(screen: Screen, circuitConfig: CircuitConfig): ScreenUi? {
    if (screen is AboutScreen) {
      return ScreenUi(aboutScreenUi())
    }
    return null
  }
}

private fun aboutScreenUi() = ui<AboutScreen.State> { About() }

@Composable
fun About(modifier: Modifier = Modifier) {
  Scaffold(
    modifier = modifier.fillMaxSize().padding(16.dp),
    content = { padding ->
      Column(
        modifier = Modifier.fillMaxSize().padding(padding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(
          modifier = Modifier.size(96.dp),
          painter = painterResource(R.drawable.star_icon),
          contentDescription = "STAR icon",
          tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(id = R.string.about_screen), textAlign = TextAlign.Justify)
      }
    }
  )
}
