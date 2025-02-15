// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.navigation

import kotlin.reflect.KClass

internal actual fun tagKey(type: KClass<*>): String? {
  return type.qualifiedName
}
