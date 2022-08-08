package com.slack.circuit.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.lifecycle.ViewModelProvider
import com.slack.circuit.Navigator
import com.slack.circuit.sample.di.CircuitViewModelProviderFactory
import com.slack.circuit.sample.petlist.PetListScreen
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

  @Inject lateinit var viewModelProviderFactory: CircuitViewModelProviderFactory
  @Inject lateinit var navigatorFactory: Navigator.Factory<*>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    (application as CircuitApp).appComponent().inject(this)

    val navigator =
      navigatorFactory.create(
        { content -> setContent { MaterialTheme { content() } } },
        onBackPressedDispatcher::onBackPressed
      )

    navigator.goTo(PetListScreen)
  }

  override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
    return viewModelProviderFactory
  }
}
