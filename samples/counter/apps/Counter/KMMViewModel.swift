//
//  KMMViewModel.swift
//  Counter
//
//  Created by Rick Clephas on 26/05/2023.
//  Copyright © 2023 orgName. All rights reserved.
//

import KMMViewModelCore
import KMMViewModelState
import counter

extension Kmm_viewmodel_coreKMMViewModel: KMMViewModel { }

extension ObservedViewModelState {
    init(wrappedValue: ViewModel) where ViewModel: Circuit_swiftuiSwiftUIPresenter<State> {
        self.init(wrappedValue: wrappedValue, \.state)
    }
}
