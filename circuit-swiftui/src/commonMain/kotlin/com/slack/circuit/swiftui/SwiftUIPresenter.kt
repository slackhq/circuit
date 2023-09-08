package com.slack.circuit.swiftui

import androidx.compose.runtime.Composable
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.swiftui.objc.CircuitSwiftUINavigatorProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

public class SwiftUIPresenter<UiState: CircuitUiState> internal constructor(
    private val swiftUINavigator: SwiftUINavigator,
    private val presenter: Presenter<UiState>
): SwiftUIPresenterProtocol() {
    public interface Factory<UiState: CircuitUiState> {
        public fun presenter(): SwiftUIPresenter<UiState>
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    public override lateinit var state: UiState
        private set

    public override var navigator: CircuitSwiftUINavigatorProtocol?
        get() = swiftUINavigator.navigator
        set(value) { swiftUINavigator.navigator = value }

    private var stateWillChangeListener: (() -> Unit)? = null

    public override fun setStateWillChangeListener(listener: () -> Unit) {
        if (this.stateWillChangeListener != null) {
            throw IllegalStateException("SwiftUIPresenter can't be wrapped more than once")
        }
        stateWillChangeListener = listener
    }

    public override fun cancel() {
        coroutineScope.cancel()
    }

    init {
        coroutineScope.launchMolecule(
            clock = RecompositionClock.Immediate,
            emitter = { value ->
                stateWillChangeListener?.invoke()
                state = value
            }
        ) {
            presenter.present()
        }
    }
}

public fun <UiState : CircuitUiState> swiftUIPresenterOf(
    body: @Composable (Navigator) -> UiState
): SwiftUIPresenter<UiState> {
    val navigator = SwiftUINavigator()
    return SwiftUIPresenter(navigator, presenterOf { body(navigator) })
}
