package com.slack.circuit.sample.di

import com.slack.circuit.sample.MainActivity
import com.slack.circuit.sample.petlist.PetListModule
import dagger.Component

@Component(modules = [CircuitModule::class, PetListModule::class, BaseUiModule::class])
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
