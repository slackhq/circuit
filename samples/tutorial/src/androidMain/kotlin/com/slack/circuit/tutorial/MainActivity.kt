// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tutorial

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.slack.circuit.tutorial.impl.tutorialOnCreate

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Replace with your own impl if following the tutorial!
    tutorialOnCreate()
  }
}
