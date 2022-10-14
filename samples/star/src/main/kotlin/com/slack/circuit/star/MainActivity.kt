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
package com.slack.circuit.star

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.ViewModelProvider
import com.slack.circuit.CircuitCompositionLocals
import com.slack.circuit.CircuitConfig
import com.slack.circuit.ContentWithOverlays
import com.slack.circuit.NavigableCircuitContent
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.push
import com.slack.circuit.rememberCircuitNavigator
import com.slack.circuit.star.di.ActivityKey
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.home.AboutScreen
import com.slack.circuit.star.home.HomeScreen
import com.slack.circuit.star.petdetail.PetDetailScreen
import com.slack.circuit.star.petlist.PetListScreen
import com.slack.circuit.star.repo.PetRepository
import com.slack.circuit.star.ui.StarTheme
import com.squareup.anvil.annotations.ContributesMultibinding
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@ContributesMultibinding(AppScope::class, boundType = Activity::class)
@ActivityKey(MainActivity::class)
class MainActivity
@Inject
constructor(
  private val viewModelProviderFactory: ViewModelProvider.Factory,
  private val circuitConfig: CircuitConfig,
  private val petRepository: PetRepository
) : AppCompatActivity() {
  private val screensHashMap = hashMapOf(
    "see-our-animals" to HomeScreen,
    "animalId" to PetDetailScreen("animalId".toLong(), null),
    "about" to AboutScreen
  )

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      StarTheme {
        // TODO why isn't the windowBackground enough so we don't need to do this?
        Surface(color = MaterialTheme.colorScheme.background) {

          // uriPattern =
          // "eway://${Screens.NOTIFICATION_DETAILS.navRoute}/{date}/{imageUrl}/{title}/{content}"
//          val endString = "see-our-animals/54090287"
//          val delim = "/"
//
//          val list = endString.split(delim)
//          Log.d("list_oncreate", "list=$list")

          val backstack = rememberSaveableBackStack { push(HomeScreen) }
          val navigator =
            rememberCircuitNavigator(backstack, onBackPressedDispatcher::onBackPressed)
          CircuitCompositionLocals(circuitConfig) {
            ContentWithOverlays { NavigableCircuitContent(navigator, backstack) }
          }
        }
      }
    }
  }

  override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
    return viewModelProviderFactory
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    setContent {
//      val endString = "see-our-animals/54090287" // animal ID for Veronica.
//      val delim = "/"
//      val list = endString.split(delim)
//      Log.d("list_onNewIntent", "list=$list")
      val backstack = rememberSaveableBackStack { push(HomeScreen) } // Temp. because this is pushing two home screens.
      intent.let {
        val query = it?.data?.query
        Log.d("query_intent", "q=$query")
        val queryArray = query?.split("/")
        queryArray?.forEach { segment ->
          var screen = screensHashMap[segment]
          if (segment.startsWith("animalId")) {
            screen = screensHashMap[segment.replace("animalId", "")]
            if (screen is PetDetailScreen) {
              val animal = runBlocking { petRepository.getAnimal(segment.toLong()) }
              screen = PetDetailScreen(segment.toLong(), animal?.url)
            }
          }
          if (screen != null) {
            backstack.push(screen)
          }
        }
      }
      val navigator = rememberCircuitNavigator(backstack, onBackPressedDispatcher::onBackPressed)
      CircuitCompositionLocals(circuitConfig) {
        ContentWithOverlays { NavigableCircuitContent(navigator, backstack) }
      }
    }
  }
}
