//
//  ComposeViewController.swift
//  starIOS
//
//  Created by Zac on 2025-09-02.
//

import SwiftUI
import Star

struct MainViewController: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return IosMainKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
