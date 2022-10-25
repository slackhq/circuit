package com.slack.circuit.codegen

import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.TypeSpec

public interface CircuitProcessingExtension {
  public fun process(spec: TypeSpec, ksAnnotated: KSAnnotated, type: FactoryType): TypeSpec
}