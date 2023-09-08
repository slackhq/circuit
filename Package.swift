// swift-tools-version:5.7
import PackageDescription

let package = Package(
    name: "Circuit",
    platforms: [.iOS(.v16)],
    products: [
        .library(
            name: "CircuitRuntime",
            targets: ["CircuitRuntime"]
        ),
        .library(
            name: "CircuitSwiftUI",
            targets: ["CircuitSwiftUI"]
        )
    ],
    targets: [
        .target(
            name: "CircuitRuntimeObjC",
            path: "CircuitRuntimeObjC",
            publicHeadersPath: "."
        ),
        .target(
            name: "CircuitRuntime",
            dependencies: [.target(name: "CircuitRuntimeObjC")],
            path: "CircuitRuntime"
        ),
        .target(
            name: "CircuitSwiftUI",
            dependencies: [.target(name: "CircuitRuntime")],
            path: "CircuitSwiftUI"
        )
    ],
    swiftLanguageVersions: [.v5]
)
