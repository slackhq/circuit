// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tutorial

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.slack.circuit.tutorial.intro.introTutorialOnCreate

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Set edge to edge + dark status bar icons
    enableEdgeToEdge()
    window
      ?.let { WindowCompat.getInsetsController(it, window.decorView) }
      ?.isAppearanceLightStatusBars = true

    // TODO replace with your own impl if following the tutorial!
    introTutorialOnCreate()
  }
}
