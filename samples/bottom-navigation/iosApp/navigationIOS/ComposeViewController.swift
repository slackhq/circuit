//
//  ComposeViewController.swift
//  navigationIOS
//
//  Created by Josh on 2025-08-23.
//

import SwiftUI
import BottomNavigationSample


struct MainViewController: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return IosMainKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
