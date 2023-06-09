import SwiftUI
import counter

struct CounterView: View {
    
  var state: CounterScreenState

  var body: some View {
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
        Button("Prime?") {
          state.eventSink(CounterScreenEventGoTo(screen: IosPrimeScreen(number: state.count)))
        }.padding()
      }
      .navigationBarTitle("Counter")
  }
}

struct CounterView_Previews: PreviewProvider {
  static var previews: some View {
      CounterView(state: CounterScreenState(count: 0, eventSink: { _ in }))
  }
}
