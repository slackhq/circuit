//
//  CircuitNavigationStack.swift
//  CircuitSwiftUI
//
//  Created by Rick Clephas on 27/05/2023.
//

import CircuitRuntime
import SwiftUI

public struct CircuitNavigationStack<Content: View>: View {
    
    @ObservedObject private var navigator: CircuitNavigator
    private let content: (NSObject) -> Content
    
    public init(_ navigator: CircuitNavigator, @ViewBuilder _ content: @escaping (_ screen: NSObject) -> Content) {
        self._navigator = ObservedObject(wrappedValue: navigator)
        self.content = content
    }
    
    public var body: some View {
        NavigationStack(path: $navigator.path) {
            screen(navigator.root).navigationDestination(for: NSObject.self) { path in
                screen(path)
            }
        }.environmentObject(navigator)
    }
    
    private func screen(_ screen: NSObject) -> some View {
        content(screen).id(screen)
    }
}
