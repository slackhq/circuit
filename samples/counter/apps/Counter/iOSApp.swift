import SwiftUI
import counter
import CircuitRuntime
import CircuitSwiftUI

@main
struct iOSApp: App {
    
    @StateObject private var navigator = CircuitNavigator(IosCounterScreen.shared)
    
	var body: some Scene {
		WindowGroup {
            CircuitNavigationStack(navigator) { screen in
                switch screen {
                case let screen as IosCounterScreen:
                    CircuitView(screen.presenter(), CounterView.init)
                case let screen as IosPrimeScreen:
                    CircuitView(screen.presenter(), PrimeView.init)
                default:
                    fatalError("Unsupported screen: \(screen)")
                }
            }
        }
	}
}
