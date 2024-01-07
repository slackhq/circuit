// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:Suppress(
  "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING"
)

package com.slack.circuit.star.parcel

import android.os.Parcel
import android.os.Parcelable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

actual typealias CommonParcelize = Parcelize

actual typealias CommonParcelable = Parcelable

actual typealias CommonParceler<T> = Parceler<T>

actual typealias CommonTypeParceler<T, P> = TypeParceler<T, P>

@Suppress("DEPRECATION") // Will use the non-deprecated one when it works
actual object ImmutableListParceler : Parceler<ImmutableList<*>> {
  override fun create(parcel: Parcel): ImmutableList<*> {
    return parcel.readArrayList(ImmutableList::class.java.classLoader)?.toImmutableList()
      ?: persistentListOf<Any>()
  }

  override fun ImmutableList<*>.write(parcel: Parcel, flags: Int) {
    parcel.writeList(this)
  }
}

actual object ImmutableSetParceler : Parceler<ImmutableSet<*>> {
  override fun create(parcel: Parcel): ImmutableSet<*> {
    return ImmutableListParceler.create(parcel).toImmutableSet()
  }

  override fun ImmutableSet<*>.write(parcel: Parcel, flags: Int) {
    with(ImmutableListParceler) { toImmutableList().write(parcel, flags) }
  }
}
