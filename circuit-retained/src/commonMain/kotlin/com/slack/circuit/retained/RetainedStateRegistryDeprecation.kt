// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

internal const val RETAINED_STATE_REGISTRY_DEPRECATION =
  "Circuit no longer reads RetainedStateRegistry. Retained values are held in a " +
    "RetainedValuesStore; install one at the root with RetainedValuesStoreProvider."

internal const val RETAINED_STATE_HOLDER_DEPRECATION =
  "Circuit no longer reads RetainedStateRegistry. Use retainRetainedValuesStoreRegistry() to " +
    "scope retained values per key."
