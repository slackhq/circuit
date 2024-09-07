// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.kotlininject

import kotlin.reflect.KClass
import me.tatarka.inject.annotations.Scope

abstract class AppScope private constructor()

@Scope annotation class SingleIn(val scope: KClass<*>)
