// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.internal.runtime

// For Android Parcelable
actual interface Parcelable

// For Android @IgnoreOnParcel
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
actual annotation class IgnoreOnParcel actual constructor()
