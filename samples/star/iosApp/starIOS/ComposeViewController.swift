//
//  ComposeViewController.swift
//  starIOS
//
//  Created by Zac on 2025-09-02.
//

import SwiftUI
import StarKt

struct MainViewController: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return StarUiViewControllerKt.makeUiViewController(graph: AppGraphCompanion.shared.create())
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
