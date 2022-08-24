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

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.slack.circuit.Circuit
import com.slack.circuit.navigator
import com.slack.circuit.sample.di.ActivityKey
import com.slack.circuit.sample.di.AppScope
import com.slack.circuit.sample.petlist.PetListScreen
import com.slack.circuit.sample.ui.StarTheme
import com.squareup.anvil.annotations.ContributesMultibinding
import javax.inject.Inject

@ContributesMultibinding(AppScope::class, boundType = Activity::class)
@ActivityKey(MainActivity::class)
class MainActivity
@Inject
constructor(
  private val viewModelProviderFactory: ViewModelProvider.Factory,
  private val circuit: Circuit
) : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

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
