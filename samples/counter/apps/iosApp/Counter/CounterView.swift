//
//  ContentView.swift
//  Counter
//
//  Created by Zac Sweers on 1/4/24.
//

import counter
import SwiftUI

// TODO: need factories for this
struct CounterView: View {
  @ObservedObject var presenter: SwiftPresenter<CounterScreenState>

  init(presenter: SwiftPresenter<CounterScreenState>) {
    self.presenter = presenter
  }

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

// Primary entry point to create a UiViewController for a given presenter/ui factory
// TODO
//  - can we call this from Kotlin?
//  - can we pass a viewFactory in from kotlin or must each of them be separate?
//  - can SKIE paper over loss of generics in interfaces?
@objc class CircuitUiViewController: UIViewController {
  private let viewFactory: () -> AnyView

  init(viewFactory: @escaping () -> any View) {
    self.viewFactory = {
      AnyView(viewFactory())
    }
    super.init(nibName: nil, bundle: nil)
  }
                 
  @available(*, unavailable)
  required init?(coder _: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  override func viewDidLoad() {
    super.viewDidLoad()
    let circuitView = viewFactory()
    let hostingController = UIHostingController(rootView: circuitView)
    addChild(hostingController)
    view.addSubview(hostingController.view)
    hostingController.view.frame = view.bounds
    hostingController.didMove(toParent: self)
  }
}

@objc class SwiftPresenterFactory : NSObject {
  @objc func createSwiftPresenter(delegate: Circuit_runtime_presenterPresenter) -> SwiftPresenterType {
    return SwiftPresenter<AnyObject>(delegate: delegate)
  }
}

@objc protocol SwiftPresenterType {
}

// TODO: we hide all this behind the Circuit UI interface somehow? Then we can pass it state only
class SwiftPresenter<T: AnyObject>: ObservableObject, SwiftPresenterType {
  @Published
  private(set) var state: T?
  private let delegate: FlowPresenter<T>

  // TODO: the raw type here is not nice. Will Kotlin eventually expose these? Maybe we should
  //  generate wrappers?
  init(delegate: Circuit_runtime_presenterPresenter) {
    self.delegate = FlowPresenter(delegate: delegate)
  }

  @MainActor
  func activate() async {
    for await state in delegate.state {
      self.state = state
    }
  }
}

// TODO how do previews work if views require input presenter params?
// struct ContentView_Previews: PreviewProvider {
//   static var previews: some View {
//     CounterView()
//   }
// }
