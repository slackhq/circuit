package com.slack.circuit.codegen.annotations

import com.slack.circuit.runtime.screen.Screen
import kotlin.reflect.KClass
import software.amazon.lastmile.kotlin.inject.anvil.extend.ContributingAnnotation

/**
 * JS-specific [CircuitInject].
 *
 * For more general information about this annotation, see
 * [com.slack.circuit.codegen.annotations.CircuitInject]
 */
@ContributingAnnotation
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
public actual annotation class CircuitInject(
  actual val screen: KClass<out Screen>,
  actual val scope: KClass<*>,
)