package com.slack.circuit.sample

import android.app.Application
import com.slack.circuit.sample.di.AppComponent

class CircuitApp : Application() {

  private lateinit var appComponent: AppComponent

  override fun onCreate() {
    super.onCreate()
    appComponent = AppComponent.create()
  }

  fun appComponent() = appComponent
}
