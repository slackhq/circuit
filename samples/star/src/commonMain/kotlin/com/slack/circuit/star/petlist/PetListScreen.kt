// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.star.parcel.CommonParcelize
import kotlinx.collections.immutable.ImmutableList

@CommonParcelize
data object PetListScreen : Screen {
  sealed interface State : CircuitUiState {
    val isRefreshing: Boolean

    data object Loading : State {
      override val isRefreshing: Boolean = false
    }

    data class NoAnimals(override val isRefreshing: Boolean) : State

    data class Success(
      val animals: ImmutableList<PetListAnimal>,
      override val isRefreshing: Boolean,
      val filters: Filters = Filters(),
      val isUpdateFiltersModalShowing: Boolean = false,
      val eventSink: (Event) -> Unit = {},
    ) : State
  }

  sealed interface Event : CircuitUiEvent {
    data class ClickAnimal(val petId: Long, val photoUrlMemoryCacheKey: String?) : Event

    data object Refresh : Event

    data object UpdateFilters : Event

    data class UpdatedFilters(val newFilters: Filters) : Event
  }
}
