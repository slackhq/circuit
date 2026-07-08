// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen

import com.google.devtools.ksp.processing.KSPLogger

internal data class CircuitOptions(
  val mode: CodegenMode,
  val lenient: Boolean,
  val useJavaxOnly: Boolean,
) {
  companion object {
    const val MODE = "circuit.codegen.mode"
    const val LENIENT = "circuit.codegen.lenient"
    const val USE_JAVAX_ONLY = "circuit.codegen.useJavaxOnly"

    // Legacy option keys from the standalone subcircuit-codegen artifact, which is now a relocation
    // pointer to circuit-codegen. Read as a fallback so existing `subcircuit.codegen.*` args keep
    // working; the `circuit.codegen.*` keys take precedence.
    private const val LEGACY_MODE = "subcircuit.codegen.mode"
    private const val LEGACY_LENIENT = "subcircuit.codegen.lenient"
    private const val LEGACY_USE_JAVAX_ONLY = "subcircuit.codegen.useJavaxOnly"

    internal val UNKNOWN =
      CircuitOptions(CodegenMode.UNKNOWN, lenient = false, useJavaxOnly = false)

    fun load(options: Map<String, String>, logger: KSPLogger): CircuitOptions {
      val mode =
        (options[MODE] ?: options[LEGACY_MODE]).let { mode ->
          if (mode == null) {
            CodegenMode.ANVIL
          } else {
            CodegenMode.entries.find { it.name.equals(mode, ignoreCase = true) }
              ?: run {
                logger.error("Unrecognised option for codegen mode \"$mode\".")
                return UNKNOWN
              }
          }
        }

      if (mode == CodegenMode.UNKNOWN) {
        logger.error("Specifying \"$mode\" as a Circuit code gen mode is prohibited.")
        return UNKNOWN
      }

      val lenient = (options[LENIENT] ?: options[LEGACY_LENIENT])?.toBoolean() ?: false
      val useJavaxOnly =
        (options[USE_JAVAX_ONLY] ?: options[LEGACY_USE_JAVAX_ONLY])?.toBoolean() ?: false
      return CircuitOptions(mode = mode, lenient = lenient, useJavaxOnly = useJavaxOnly)
    }
  }
}
