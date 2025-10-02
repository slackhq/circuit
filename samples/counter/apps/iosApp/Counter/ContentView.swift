//
//  ContentView.swift
//  Counter
//
//  Created by Zac Sweers on 1/4/24.
//

import CounterKt
import SwiftUI
import KMPNativeCoroutinesAsync

struct ContentView: View {
  @ObservedObject var presenter = SwiftPresenter<CounterScreenState>(delegate: SwiftSupportKt.doNewCounterPresenter())

  var body: some View {
    NavigationView {
      VStack(alignment: .center) {
        let count = presenter.state?.count ?? 0
        Text("Count \(count)")
          .foregroundStyle(count >= 0 ? Color.primary : Color.red)
          .font(.system(size: 36))
        HStack(spacing: 10) {
          Button {
            presenter.state?.eventSink(CounterScreenEventDecrement.shared)
          } label: {
            Text("-")
              .font(.system(size: 36, weight: .black, design: .monospaced))
          }
          .padding()
          .foregroundColor(.white)
          .background(Color.blue)

          Button {
            presenter.state?.eventSink(CounterScreenEventIncrement.shared)
          } label: {
            Text("+")
              .font(.system(size: 36, weight: .black, design: .monospaced))
          }
          .padding()
          .foregroundColor(.white)
          .background(Color.blue)
        }
      }
      .navigationBarTitle("Counter")
    }.task {
      await presenter.activate()
    }
  }
}

// TODO: we hide all this behind the Circuit UI interface somehow? Then we can pass it state only
class SwiftPresenter<T: AnyObject>: ObservableObject {
  @Published
  private(set) var state: T?
  private let delegate: SupportSwiftPresenter<T>

  // TODO: the raw type here is not nice. Will Kotlin eventually expose these? Maybe we should
  //  generate wrappers?
  init(delegate: Circuit_runtime_presenterPresenter) {
    self.delegate = SupportSwiftPresenter(delegate: delegate)
  }

  @MainActor
  func activate() async {
    let sequence = asyncSequence(for: delegate.stateFlow)
      do {
          for try await state in sequence {
            self.state = state
          }
      } catch {
          print("Failed with error: \(error)")
      }
  }
}

struct ContentView_Previews: PreviewProvider {
  static var previews: some View {
    ContentView()
  }
}
