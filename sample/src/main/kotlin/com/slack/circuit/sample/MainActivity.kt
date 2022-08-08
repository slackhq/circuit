package com.slack.circuit.sample

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.slack.circuit.Navigator
import com.slack.circuit.asContentContainer
import com.slack.circuit.sample.di.CircuitViewModelProviderFactory
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

  @Inject lateinit var viewModelProviderFactory: CircuitViewModelProviderFactory
  @Inject lateinit var navigatorFactory: Navigator.Factory<*>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val navigator = navigatorFactory.create(asContentContainer()) {
      onBackPressedDispatcher.onBackPressed()
    }

    // TODO navigator.goTo(somewhere)
  }

  override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
    return viewModelProviderFactory
  }
}