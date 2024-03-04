// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.joshafeinberg.circuitkotlininject.sample

import com.slack.circuit.codegen.annotations.MergeCircuitComponent
import com.slack.circuit.foundation.di.CircuitComponent
import me.tatarka.inject.annotations.Component

@Suppress("UNUSED_PARAMETER")
@MergeCircuitComponent(AppScope::class)
abstract class AppScopeCircuitComponent(@Component appComponent: AppComponent) : CircuitComponent
