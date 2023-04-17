package com.slack.circuit.foundation

import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.internal.StatelessUiState
import com.slack.circuit.runtime.presenter.StaticPresenter

@OptIn(InternalCircuitApi::class)
internal object StaticStatelessPresenter : StaticPresenter<StatelessUiState>({ StatelessUiState })
