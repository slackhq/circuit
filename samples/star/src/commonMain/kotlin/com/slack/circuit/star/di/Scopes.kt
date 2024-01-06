// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import javax.inject.Scope
import kotlin.reflect.KClass

@Scope annotation class SingleIn(val scope: KClass<*>)

class AppScope private constructor()
