//
//  Circuit.swift
//  Counter
//
//  Created by Rick Clephas on 26/05/2023.
//  Copyright © 2023 orgName. All rights reserved.
//

import counter
import SwiftUI
import CircuitRuntime
import CircuitSwiftUI

extension Circuit_swiftuiSwiftUIPresenter: CircuitPresenter { }

extension CircuitView {
    init(_ presenter: @autoclosure @escaping () -> Presenter,
         @ViewBuilder _ content: @escaping (State) -> Content
    ) where Presenter: Circuit_swiftuiSwiftUIPresenter<State> {
        self.init(presenter(), \.state, content)
    }
}
