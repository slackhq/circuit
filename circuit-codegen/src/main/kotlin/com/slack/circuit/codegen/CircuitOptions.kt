// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen

import com.google.devtools.ksp.processing.KSPLogger

internal data class CircuitOptions(
  val mode: CodegenMode,
  val lenient: Boolean,
  val useJavax: Boolean,
) {
  companion object {
    const val MODE = "circuit.codegen.mode"
    const val LENIENT = "circuit.codegen.lenient"
    const val USE_JAVAX = "circuit.codegen.useJavax"

    internal val UNKNOWN = CircuitOptions(CodegenMode.UNKNOWN, lenient = false, useJavax = false)

    fun load(options: Map<String, String>, logger: KSPLogger): CircuitOptions {
      val mode =
        options[MODE].let { mode ->
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

      val lenient = options[LENIENT]?.toBoolean() ?: false
      val useJavax = options[USE_JAVAX]?.toBoolean() ?: false
      return CircuitOptions(mode = mode, lenient = lenient, useJavax = useJavax)
    }
  }
}
