package com.slack.circuit.backstack

/**
 * This is specifically a get() rather than a statically initialized property. The Kotlin/Native
 * optimizer seems to trip up otherwise: https://github.com/slackhq/circuit/issues/1075
 */
internal actual val defaultBackStackRecordLocalProviders:
  List<BackStackRecordLocalProvider<BackStack.Record>>
  get() = emptyList()
