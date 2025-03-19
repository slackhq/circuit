// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.internal.runtime

// For Android @Parcelize
@Target(AnnotationTarget.CLASS) @Retention(AnnotationRetention.BINARY) annotation class Parcelize

// For Android @IgnoreOnParcel
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
expect annotation class IgnoreOnParcel()

// For Android Parcelable
expect interface Parcelable
