// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.parcel

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

// For Android @Parcelize
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
expect annotation class CommonParcelize()

// For Android Parcelable
expect interface CommonParcelable

// For Android @TypeParceler
@Retention(AnnotationRetention.SOURCE)
@Repeatable
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
expect annotation class CommonTypeParceler<T, P : CommonParceler<in T>>()

// For Android Parceler
expect interface CommonParceler<T>

expect object ImmutableListParceler : CommonParceler<ImmutableList<*>>

expect object ImmutableSetParceler : CommonParceler<ImmutableSet<*>>
