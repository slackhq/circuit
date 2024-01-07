// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.parcel

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

@Retention(BINARY) @Target(CLASS) actual annotation class CommonParcelize

@Target(CLASS, PROPERTY)
@Repeatable
@Retention(SOURCE)
actual annotation class CommonTypeParceler<T, P : CommonParceler<in T>>

actual interface CommonParcelable

actual interface CommonParceler<T>

actual object ImmutableListParceler : CommonParceler<ImmutableList<*>>

actual object ImmutableSetParceler : CommonParceler<ImmutableSet<*>>
