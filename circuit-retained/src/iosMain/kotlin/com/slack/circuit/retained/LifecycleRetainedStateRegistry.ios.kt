package com.slack.circuit.retained

import androidx.compose.runtime.Composable

@Composable
public actual fun lifecycleRetainedStateRegistry(key: String): RetainedStateRegistry =
  lifecycleRetainedStateRegistry(key, RetainedStateRegistryViewModel.Factory)
