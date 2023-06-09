//
//  CircuitView.swift
//  CircuitSwiftUI
//
//  Created by Rick Clephas on 27/05/2023.
//

import CircuitRuntime
import KMMViewModelSwiftUI
import SwiftUI

public struct CircuitView<Presenter: CircuitPresenter, State: AnyObject, Content: View>: View {
    
    @EnvironmentObject private var navigator: CircuitNavigator
    @StateViewModel private var presenter: Presenter
    private var stateKeyPath: KeyPath<Presenter, State>
    private var content: (State) -> Content
    
    public init(_ presenter: @autoclosure @escaping () -> Presenter,
         _ stateKeyPath: KeyPath<Presenter, State>,
         @ViewBuilder _ content: @escaping (State) -> Content
    ) {
        self._presenter = StateViewModel(wrappedValue: presenter())
        self.stateKeyPath = stateKeyPath
        self.content = content
    }
    
    public var body: some View {
        content(presenter[keyPath: stateKeyPath]).onAppear {
            presenter.navigator = navigator
        }
    }
}
