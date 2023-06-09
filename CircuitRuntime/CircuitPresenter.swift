//
//  CircuitPresenter.swift
//  CircuitRuntime
//
//  Created by Rick Clephas on 27/05/2023.
//

import CircuitRuntimeObjC
import KMMViewModelCore

public protocol CircuitPresenter: KMMViewModel {
    var navigator: CircuitSwiftUINavigator? { get set }
}
