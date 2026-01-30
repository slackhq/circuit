// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.internal

import androidx.compose.runtime.Composable
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationEventHandler
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.LocalCircuit
import com.slack.circuit.runtime.InternalCircuitApi

/**
 * When lenient we want to default disable the navigation handler when there is no
 * [LocalNavigationEventDispatcherOwner] as [NavigationEventHandler] requires a non-null owner.
 *
 * Setting [Circuit.lenientNavigationEventDispatcherOwner] to false disables this behavior.
 *
 * @return true if the nav event handler should be enabled.
 */
@Composable
@InternalCircuitApi
public fun shouldEnableNavEventHandler(): Boolean {
  val isNotLenient = LocalCircuit.current?.lenientNavigationEventDispatcherOwner == false
  return isNotLenient || LocalNavigationEventDispatcherOwner.current != null
}
