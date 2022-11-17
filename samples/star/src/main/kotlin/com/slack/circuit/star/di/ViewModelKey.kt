// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import androidx.lifecycle.ViewModel
import dagger.MapKey
import kotlin.reflect.KClass

/**
 * A Dagger multi-binding key used for registering a [ViewModel] into the top level dagger graphs.
 */
@MapKey annotation class ViewModelKey(val value: KClass<out ViewModel>)
