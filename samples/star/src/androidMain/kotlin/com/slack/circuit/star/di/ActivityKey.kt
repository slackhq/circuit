// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import android.app.Activity
import dev.zacsweers.metro.MapKey
import kotlin.reflect.KClass

/**
 * A Dagger multi-binding key used for registering a [Activity] into the top level dagger graphs.
 */
@MapKey annotation class ActivityKey(val value: KClass<out Activity>)
