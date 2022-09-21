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
package com.slack.circuit.sample.petlist

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.ScreenUi
import com.slack.circuit.Ui
import com.slack.circuit.sample.R
import com.slack.circuit.sample.di.AppScope
import com.slack.circuit.ui
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize

@Parcelize
class AboutScreen : Screen {
  object State : CircuitUiState
  object Event : CircuitUiEvent
}

@ContributesMultibinding(AppScope::class)
class AboutPresenterFactory
@Inject
constructor(private val aboutPresenterFactory: AboutPresenter.Factory) : Presenter.Factory {
  override fun create(screen: Screen, navigator: Navigator): Presenter<*, *>? {
    if (screen is AboutScreen) return aboutPresenterFactory.create()
    return null
  }
}

class AboutPresenter @AssistedInject constructor() :
  Presenter<AboutScreen.State, AboutScreen.Event> {
  @Composable
  override fun present(events: Flow<AboutScreen.Event>): AboutScreen.State {
    return AboutScreen.State
  }

  @AssistedFactory
  interface Factory {
    fun create(): AboutPresenter
  }
}

@ContributesMultibinding(AppScope::class)
class AboutUiFactory @Inject constructor() : Ui.Factory {
  override fun create(screen: Screen): ScreenUi? {
    if (screen is AboutScreen) {
      return ScreenUi(aboutScreenUi())
    }
    return null
  }
}

private fun aboutScreenUi() = ui<AboutScreen.State, Nothing> { _, _ -> About() }

@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun About() {
  Scaffold(
    content = {
      Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text = stringResource(id = R.string.about_screen))
      }
    }
  )
}
