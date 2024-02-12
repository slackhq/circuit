// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.joshafeinberg.circuitkotlininject.sample

import com.slack.circuit.codegen.annotations.MergeCircuitComponent
import com.slack.circuit.foundation.di.CircuitComponent

@MergeCircuitComponent<AppComponent>(AppScope::class)
interface AppScopeCircuitComponent : CircuitComponent
