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
package com.slack.circuit.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.slack.circuit.Circuit
import com.slack.circuit.navigator
import com.slack.circuit.sample.di.CircuitViewModelProviderFactory
import com.slack.circuit.sample.petlist.PetListScreen
import com.slack.circuit.sample.ui.StarTheme
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

  @Inject lateinit var viewModelProviderFactory: CircuitViewModelProviderFactory
  @Inject lateinit var circuit: Circuit

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    (application as StarApp).appComponent().inject(this)

    val navigator =
      circuit.navigator(
        { content -> setContent { StarTheme { content() } } },
        onBackPressedDispatcher::onBackPressed
      )

    navigator.goTo(PetListScreen)
  }

  override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
    return viewModelProviderFactory
  }
}
