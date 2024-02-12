// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.joshafeinberg.circuitkotlininject.sample

import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@Component
abstract class AppComponent {
  @Provides
  fun providesString(): String {
    return "Injected String!"
  }
}
