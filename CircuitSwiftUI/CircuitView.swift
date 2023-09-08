//
//  CircuitView.swift
//  CircuitSwiftUI
//
//  Created by Rick Clephas on 27/05/2023.
//

import CircuitRuntime
import SwiftUI

public struct CircuitView<Presenter: CircuitPresenter, State: Any, Content: View>: View {
    
    @EnvironmentObject private var navigator: CircuitNavigator
    @StateObject private var observableObject: ObservablePresenter<Presenter>
    private var stateKeyPath: KeyPath<Presenter, State>
    private var content: (State) -> Content
    
    public init(_ presenter: @autoclosure @escaping () -> Presenter,
                @ViewBuilder _ content: @escaping (State) -> Content,
                _ stateKeyPath: KeyPath<Presenter, State> = \Presenter.state
    ) {
        self._observableObject = StateObject(wrappedValue: observablePresenter(for: presenter()))
        self.stateKeyPath = stateKeyPath
        self.content = content
    }
    
    public var body: some View {
        content(observableObject.presenter[keyPath: stateKeyPath]).onAppear {
            observableObject.presenter.navigator = navigator
        }
    }
}
