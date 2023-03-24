// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.common

import android.os.Parcel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.parcelize.Parceler

@Suppress("DEPRECATION") // Will use the non-deprecated one when it works
object ImmutableListParceler : Parceler<ImmutableList<*>> {
  override fun create(parcel: Parcel): ImmutableList<*> {
    return parcel.readArrayList(ImmutableList::class.java.classLoader)?.toImmutableList()
      ?: persistentListOf<Any>()
  }

  override fun ImmutableList<*>.write(parcel: Parcel, flags: Int) {
    parcel.writeList(this)
  }
}

object ImmutableSetParceler : Parceler<ImmutableSet<*>> {
  override fun create(parcel: Parcel): ImmutableSet<*> {
    return ImmutableListParceler.create(parcel).toImmutableSet()
  }

  override fun ImmutableSet<*>.write(parcel: Parcel, flags: Int) {
    with(ImmutableListParceler) { toImmutableList().write(parcel, flags) }
  }
}
