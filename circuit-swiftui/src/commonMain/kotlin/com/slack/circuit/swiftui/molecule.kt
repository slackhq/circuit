package com.slack.circuit.swiftui

import androidx.compose.runtime.Composable
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.rickclephas.kmm.viewmodel.MutableStateFlow
import com.rickclephas.kmm.viewmodel.ViewModelScope
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Identical to the molecule implementation, but with a KMM-ViewModel [MutableStateFlow].
 * https://github.com/cashapp/molecule/blob/c902f7f60022911bf0cc6940cf86f3ff07c76591/molecule-runtime/src/commonMain/kotlin/app/cash/molecule/molecule.kt#L102
 */
internal fun <T> ViewModelScope.launchMolecule(
    clock: RecompositionClock,
    body: @Composable () -> T,
): StateFlow<T> {
    var flow: MutableStateFlow<T>? = null
    coroutineScope.launchMolecule(
        clock = clock,
        emitter = { value ->
            val outputFlow = flow
            if (outputFlow != null) {
                outputFlow.value = value
            } else {
                flow = MutableStateFlow(this, value)
            }
        },
        body = body,
    )
    return flow!!
}
