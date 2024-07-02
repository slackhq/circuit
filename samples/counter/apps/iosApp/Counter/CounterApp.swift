//
//  CounterApp.swift
//  Counter
//
//  Created by Zac Sweers on 1/4/24.
//

import SwiftUI
import counter

@main
struct CounterApp: App {
  var body: some Scene {
    WindowGroup {
        CounterView(presenter: SwiftPresenter<CounterScreenState>(delegate: SwiftSupportKt.doNewCounterPresenter()))
    }
  }
}
