// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@Target(CONSTRUCTOR, FUNCTION, PROPERTY_SETTER) expect annotation class Inject()

@Target(CONSTRUCTOR) expect annotation class AssistedInject()

@Target(VALUE_PARAMETER) expect annotation class Assisted(val value: String = "")

@Target(CLASS) expect annotation class AssistedFactory()
