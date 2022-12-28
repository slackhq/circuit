// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.common

import android.os.Parcel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.parcelize.Parceler

@Suppress("DEPRECATION") // Will use the non-deprecated one when it works
class ImmutableListParceler : Parceler<ImmutableList<*>> {
  override fun create(parcel: Parcel): ImmutableList<*> {
    return parcel.readArrayList(ImmutableList::class.java.classLoader)?.toImmutableList()
      ?: persistentListOf<Any>()
  }

  override fun ImmutableList<*>.write(parcel: Parcel, flags: Int) {
    parcel.writeList(this)
  }
}
