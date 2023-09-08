package com.slack.circuit.swiftui

import com.slack.circuit.swiftui.objc.CircuitSwiftUINavigatorProtocol

public sealed class SwiftUIPresenterProtocol {
    public abstract val state: Any
    public abstract var navigator: CircuitSwiftUINavigatorProtocol?
    public abstract fun setStateWillChangeListener(listener: () -> Unit)
    public abstract fun cancel()
}
