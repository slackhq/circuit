// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("PackageDirectoryMismatch")

package amazon.lastmile.inject

import com.slack.circuit.codegen.annotations.CircuitInject
import kotlin.reflect.KClass
import software.amazon.lastmile.kotlin.inject.anvil.`internal`.Origin

// TODO temporary until https://github.com/amzn/kotlin-inject-anvil/issues/24
@Origin(value = CircuitInject::class)
internal val comSlackCircuitCodegenAnnotationsCircuitInject: KClass<CircuitInject> =
  CircuitInject::class
