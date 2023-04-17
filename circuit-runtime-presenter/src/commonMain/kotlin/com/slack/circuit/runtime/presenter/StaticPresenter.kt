package com.slack.circuit.runtime.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.slack.circuit.runtime.CircuitUiState
import kotlin.LazyThreadSafetyMode.NONE

public abstract class StaticPresenter<UiState : CircuitUiState>(
  computeState: () -> UiState,
) : Presenter<CircuitUiState> {

  private val computedState: UiState by lazy(NONE, computeState)

  @ReadOnlyComposable
  @Composable
  override fun present(): UiState = computedState
}
