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
package com.slack.circuit.sample.di

import com.slack.circuit.sample.MainActivity
import com.slack.circuit.sample.petdetail.PetDetailModule
import com.slack.circuit.sample.petlist.PetListModule
import dagger.Component

@Component(
  modules =
    [CircuitModule::class, PetListModule::class, BaseUiModule::class, PetDetailModule::class]
)
interface AppComponent {
  fun inject(mainActivity: MainActivity)

  @Component.Factory
  interface Factory {
    fun create(): AppComponent
  }

  companion object {
    fun create(): AppComponent = DaggerAppComponent.factory().create()
  }
}
