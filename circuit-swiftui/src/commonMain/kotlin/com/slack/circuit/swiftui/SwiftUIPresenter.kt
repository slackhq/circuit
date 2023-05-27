package com.slack.circuit.swiftui

import androidx.compose.runtime.Composable
import app.cash.molecule.RecompositionClock
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.swiftui.objc.CircuitSwiftUINavigatorProtocol

public class SwiftUIPresenter<UiState: CircuitUiState> internal constructor(
    private val swiftUINavigator: SwiftUINavigator,
    private val presenter: Presenter<UiState>
): KMMViewModel() {
    public interface Factory<UiState: CircuitUiState> {
        public fun presenter(): SwiftUIPresenter<UiState>
    }

    private val stateFlow = viewModelScope.launchMolecule(RecompositionClock.Immediate) {
        presenter.present()
    }

    public val state: UiState get() = stateFlow.value

    public var navigator: CircuitSwiftUINavigatorProtocol?
        get() = swiftUINavigator.navigator
        set(value) { swiftUINavigator.navigator = value }
}

public fun <UiState : CircuitUiState> swiftUIPresenterOf(
    body: @Composable (Navigator) -> UiState
): SwiftUIPresenter<UiState> {
    val navigator = SwiftUINavigator()
    return SwiftUIPresenter(navigator, presenterOf { body(navigator) })
}
