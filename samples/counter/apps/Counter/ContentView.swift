import SwiftUI
import counter

struct ContentView: View {
  @ObservedObject var presenter = SwiftCounterPresenter()

  var body: some View {
    NavigationView {
      VStack(alignment: .center) {
        Text("Count \(presenter.count)")
          .font(.system(size: 36))
        HStack(spacing: 10) {
          Button(action: {
            presenter.decrement()
          }) {
            Text("-")
              .font(.system(size: 36, weight: .black, design: .monospaced))
          }
          .padding()
          .foregroundColor(.white)
          .background(Color.blue)
          Button(action: {
            presenter.increment()
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

@MainActor
class SwiftCounterPresenter: ObservableObject {
  private let delegate = FlowCounterPresenter().createSwiftCounterScreen()

  @Published var count = 0

  init() {
    delegate.subscribe { count in
      guard let count = count else { return }
      self.count = count.intValue
    }
  }

  func increment() {
    delegate.send(event: CounterScreenEventIncrement.shared)
  }

  func decrement() {
    delegate.send(event: CounterScreenEventDecrement.shared)
  }
}

struct ContentView_Previews: PreviewProvider {
  static var previews: some View {
    ContentView()
  }
}
