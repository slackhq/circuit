// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petdetail

import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.star.parcel.CommonParcelize
import com.slack.circuit.star.parcel.CommonTypeParceler
import com.slack.circuit.star.parcel.ImmutableListParceler
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@CommonParcelize
data class PetPhotoCarouselScreen(
  val name: String,
  @CommonTypeParceler<ImmutableList<String>, ImmutableListParceler>
  val photoUrls: ImmutableList<String>,
  val photoUrlMemoryCacheKey: String?,
) : Screen {
  data class State(
    val name: String,
    val photoUrls: ImmutableList<String>,
    val photoUrlMemoryCacheKey: String?,
  ) : CircuitUiState {
    companion object {
      operator fun invoke(screen: PetPhotoCarouselScreen): State {
        return State(
          name = screen.name,
          photoUrls = screen.photoUrls.toImmutableList(),
          photoUrlMemoryCacheKey = screen.photoUrlMemoryCacheKey,
        )
      }
    }
  }
}
