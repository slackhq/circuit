//
//  CircuitNavigator.swift
//  CircuitRuntime
//
//  Created by Rick Clephas on 27/05/2023.
//

import Foundation
import CircuitRuntimeObjC

public class CircuitNavigator: ObservableObject, CircuitSwiftUINavigator {
    
    @Published public var root: NSObject
    @Published public var path: [NSObject]
    
    public init(_ root: NSObject, _ path: NSObject...) {
        self.root = root
        self.path = path
    }
    
    public func goTo(screen: NSObject) {
        path.append(screen)
    }
    
    public func pop() -> NSObject? {
        return path.popLast()
    }
    
    public func resetRoot(newRoot: NSObject) -> [NSObject] {
        let oldRoot = root
        var oldPath = path
        root = newRoot
        path = []
        oldPath.insert(oldRoot, at: 0)
        return oldPath
    }
}
