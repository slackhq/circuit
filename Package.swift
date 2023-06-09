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
    dependencies: [
        .package(
            url: "https://github.com/rickclephas/KMM-ViewModel.git",
            from: "1.0.0-ALPHA-9"
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
            dependencies: [.target(name: "CircuitRuntimeObjC"), .product(name: "KMMViewModelCore", package: "KMM-ViewModel")],
            path: "CircuitRuntime"
        ),
        .target(
            name: "CircuitSwiftUI",
            dependencies: [.target(name: "CircuitRuntime"), .product(name: "KMMViewModelSwiftUI", package: "KMM-ViewModel")],
            path: "CircuitSwiftUI"
        )
    ],
    swiftLanguageVersions: [.v5]
)
