// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit.codegen

import com.google.devtools.ksp.processing.KSPLogger

internal data class SubCircuitOptions(
  val mode: SubCircuitCodegenMode,
  val lenient: Boolean,
  val useJavaxOnly: Boolean,
) {
  companion object {
    const val MODE = "subcircuit.codegen.mode"
    const val LENIENT = "subcircuit.codegen.lenient"
    const val USE_JAVAX_ONLY = "subcircuit.codegen.useJavaxOnly"

    internal val UNKNOWN =
      SubCircuitOptions(SubCircuitCodegenMode.UNKNOWN, lenient = false, useJavaxOnly = false)

    fun load(options: Map<String, String>, logger: KSPLogger): SubCircuitOptions {
      val mode =
        options[MODE].let { mode ->
          if (mode == null) {
            SubCircuitCodegenMode.ANVIL
          } else {
            SubCircuitCodegenMode.entries.find { it.name.equals(mode, ignoreCase = true) }
              ?: run {
                logger.error("Unrecognised option for codegen mode \"$mode\".")
                return UNKNOWN
              }
          }
        }

      if (mode == SubCircuitCodegenMode.UNKNOWN) {
        logger.error("Specifying \"$mode\" as a SubCircuit code gen mode is prohibited.")
        return UNKNOWN
      }

      val lenient = options[LENIENT]?.toBoolean() ?: false
      val useJavaxOnly = options[USE_JAVAX_ONLY]?.toBoolean() ?: false
      return SubCircuitOptions(mode = mode, lenient = lenient, useJavaxOnly = useJavaxOnly)
    }
  }
}
