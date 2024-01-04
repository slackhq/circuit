//
//  ContentView.swift
//  Counter
//
//  Created by Zac Sweers on 1/4/24.
//

import SwiftUI
import counter

struct ContentView: View {
  @ObservedObject var presenter = SwiftCounterPresenter()

  var body: some View {
    NavigationView {
      VStack(alignment: .center) {
        Text("Count \(presenter.state?.count ?? 0)")
          .font(.system(size: 36))
        HStack(spacing: 10) {
          Button(action: {
            presenter.state?.eventSink(CounterScreenEventDecrement.shared)
          }) {
            Text("-")
              .font(.system(size: 36, weight: .black, design: .monospaced))
          }
          .padding()
          .foregroundColor(.white)
          .background(Color.blue)
          Button(action: {
            presenter.state?.eventSink(CounterScreenEventIncrement.shared)
          }) {
            Text("+")
              .font(.system(size: 36, weight: .black, design: .monospaced))
          }
          .padding()
          .foregroundColor(.white)
          .background(Color.blue)
        }
      }
      .navigationBarTitle("Counter")
    }
  }
}

// TODO we hide all this behind the Circuit UI interface somehow? Then we can pass it state only
@MainActor
class SwiftCounterPresenter: BasePresenter<CounterScreenState> {
  init() {
    // TODO why can't swift infer these generics?
    super.init(
      delegate: SwiftSupportKt.asSwiftPresenter(SwiftSupportKt.doNewCounterPresenter())
        as! SwiftPresenter<CounterScreenState>)
  }
}

class BasePresenter<T: AnyObject>: ObservableObject {
  @Published var state: T? = nil

  init(delegate: SwiftPresenter<T>) {
    delegate.subscribe { state in
      self.state = state
    }
  }
}

struct ContentView_Previews: PreviewProvider {
  static var previews: some View {
    ContentView()
  }
}
