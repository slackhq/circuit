//
//  ObservablePresenter.swift
//  CircuitSwiftUI
//
//  Created by Rick Clephas on 06/09/2023.
//

import Foundation
import CircuitRuntime

internal class ObservablePresenter<Presenter: CircuitPresenter>: ObservableObject {
    let presenter: Presenter
    
    init(_ presenter: Presenter) {
        self.presenter = presenter
        presenter.setStateWillChangeListener { [weak self] in
            self?.objectWillChange.send()
        }
    }
    
    deinit {
        presenter.cancel()
    }
}

private var observablePresenterKey = "observablePresenter"

private class WeakObservablePresenter<Presenter: CircuitPresenter> {
    weak var observablePresenter: ObservablePresenter<Presenter>?
    init(_ observablePresenter: ObservablePresenter<Presenter>) {
        self.observablePresenter = observablePresenter
    }
}

internal func observablePresenter<Presenter: CircuitPresenter>(for presenter: Presenter) -> ObservablePresenter<Presenter> {
    if let object = objc_getAssociatedObject(presenter, &observablePresenterKey) {
        guard let observablePresenter = (object as! WeakObservablePresenter<Presenter>).observablePresenter else {
            fatalError("ObservablePresenter has been deallocated")
        }
        return observablePresenter
    } else {
        let observablePresenter = ObservablePresenter(presenter)
        let object = WeakObservablePresenter<Presenter>(observablePresenter)
        objc_setAssociatedObject(presenter, &observablePresenterKey, object, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return observablePresenter
    }
}
