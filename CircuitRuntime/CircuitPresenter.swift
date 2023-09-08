//
//  CircuitPresenter.swift
//  CircuitRuntime
//
//  Created by Rick Clephas on 27/05/2023.
//

import CircuitRuntimeObjC

public protocol CircuitPresenter: AnyObject {
    var state: Any { get }
    var navigator: CircuitSwiftUINavigator? { get set }
    func setStateWillChangeListener(listener: @escaping () -> Void)
    func cancel()
}
