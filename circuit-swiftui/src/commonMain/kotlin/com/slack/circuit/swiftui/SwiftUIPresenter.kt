package com.slack.circuit.swiftui

import androidx.compose.runtime.Composable
import app.cash.molecule.RecompositionClock
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.presenter.presenterOf

public class SwiftUIPresenter<UiState: CircuitUiState> internal constructor(
    private val presenter: Presenter<UiState>
): KMMViewModel() {

    private val stateFlow = viewModelScope.launchMolecule(RecompositionClock.Immediate) {
        presenter.present()
    }

    public val state: UiState get() = stateFlow.value
}

public fun <UiState : CircuitUiState> swiftUIPresenterOf(
    body: @Composable (Navigator) -> UiState
): SwiftUIPresenter<UiState> {
    return SwiftUIPresenter(presenterOf { body(Navigator.NoOp) })
}
