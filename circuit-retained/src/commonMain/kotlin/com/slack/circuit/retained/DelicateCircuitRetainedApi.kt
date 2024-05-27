package com.slack.circuit.retained

import kotlin.annotation.AnnotationTarget.FUNCTION

/** Indicates that the annotated API is delicate and should be used carefully. */
@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
@Target(FUNCTION)
public annotation class DelicateCircuitRetainedApi