import SwiftUI
import counter
import KMMViewModelState

struct ContentView: View {
  @ObservedViewModelState var state = PresenterFactory.shared.counterPresenter()

  var body: some View {
    NavigationView {
      VStack(alignment: .center) {
        Text("Count \(state.count)")
          .font(.system(size: 36))
        HStack(spacing: 10) {
          Button(action: {
            state.eventSink(CounterScreenEventDecrement.shared)
          }) {
            Text("-")
              .font(.system(size: 36, weight: .black, design: .monospaced))
          }
          .padding()
          .foregroundColor(.white)
          .background(Color.blue)
          Button(action: {
            state.eventSink(CounterScreenEventIncrement.shared)
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

struct ContentView_Previews: PreviewProvider {
  static var previews: some View {
    ContentView()
  }
}
