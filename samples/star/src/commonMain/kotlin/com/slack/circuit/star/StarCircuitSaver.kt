// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import androidx.savedstate.serialization.SavedStateConfiguration
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.serialization.SerializableCircuitSaver
import com.slack.circuit.star.benchmark.ListBenchmarksItemScreen
import com.slack.circuit.star.benchmark.ListBenchmarksScreen
import com.slack.circuit.star.home.AboutScreen
import com.slack.circuit.star.home.HomeScreen
import com.slack.circuit.star.imageviewer.ImageViewerScreen
import com.slack.circuit.star.navigation.OpenUrlScreen
import com.slack.circuit.star.navigation.openUrlScreenSerializer
import com.slack.circuit.star.petdetail.PetDetailScreen
import com.slack.circuit.star.petdetail.PetPhotoCarouselScreen
import com.slack.circuit.star.petlist.FiltersScreen
import com.slack.circuit.star.petlist.PetListScreen
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

internal val starCircuitSaver: CircuitSaver =
  SerializableCircuitSaver(
    SavedStateConfiguration {
      serializersModule = SerializersModule {
        // TODO: Replace these manual registrations with Circuit serialization code generation.
        polymorphic(CircuitSaveable::class) {
          subclass(ListBenchmarksScreen::class)
          subclass(ListBenchmarksItemScreen::class)
          subclass(AboutScreen::class)
          subclass(HomeScreen::class)
          subclass(ImageViewerScreen::class)
          subclass(OpenUrlScreen::class, openUrlScreenSerializer)
          subclass(PetDetailScreen::class)
          subclass(PetPhotoCarouselScreen::class)
          subclass(FiltersScreen::class)
          subclass(FiltersScreen.Result::class)
          subclass(PetListScreen::class)
        }
      }
    }
  )
