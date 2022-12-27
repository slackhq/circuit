package com.slack.circuit.sample.navigation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.slack.circuit.CircuitCompositionLocals
import com.slack.circuit.CircuitConfig
import com.slack.circuit.NavigableCircuitContent
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.push
import com.slack.circuit.rememberCircuitNavigator

class MainActivity : AppCompatActivity() {
  private val circuitConfig: CircuitConfig =
    CircuitConfig.Builder()
      .addPresenterFactory(ParentPresenterFactory, ChildPresenterFactory)
      .addUiFactory(ParentUiFactory, ChildUiFactory)
      .addScreenReducer { oldScreen, result ->
        when {
          oldScreen is ParentScreen && result is ParentScreen.ChildResult ->
            ParentScreen(name = result.name)
          else -> null
        }
      }
      .build()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      val context = LocalContext.current
      val colorScheme =
        if (isSystemInDarkTheme()) {
          dynamicDarkColorScheme(context)
        } else {
          dynamicLightColorScheme(context)
        }

      val systemUiController = rememberSystemUiController()
      systemUiController.setSystemBarsColor(color = colorScheme.primaryContainer)

      val backstack = rememberSaveableBackStack { push(ParentScreen()) }
      val navigator = rememberCircuitNavigator(backstack)

      MaterialTheme(colorScheme = colorScheme) {
        CircuitCompositionLocals(circuitConfig) { NavigableCircuitContent(navigator, backstack) }
      }
    }
  }
}
