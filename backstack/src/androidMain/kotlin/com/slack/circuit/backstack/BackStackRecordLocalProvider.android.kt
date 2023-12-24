package com.slack.circuit.backstack

internal actual val defaultBackStackRecordLocalProviders:
  List<BackStackRecordLocalProvider<BackStack.Record>> =
  listOf(SaveableStateRegistryBackStackRecordLocalProvider, ViewModelBackStackRecordLocalProvider)
