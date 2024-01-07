package com.slack.circuit.star.petlist

import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.Size
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import com.slack.circuit.star.parcel.CommonParcelize
import com.slack.circuit.star.parcel.CommonParcelable
import com.slack.circuit.star.parcel.CommonTypeParceler
import com.slack.circuit.star.parcel.ImmutableListParceler
import com.slack.circuit.star.parcel.ImmutableSetParceler

@CommonParcelize
class Filters(
  @CommonTypeParceler<ImmutableSet<Gender>, ImmutableSetParceler>
  val genders: ImmutableSet<Gender> = Gender.entries.asIterable().toImmutableSet(),
  @CommonTypeParceler<ImmutableSet<Size>, ImmutableSetParceler>
  val sizes: ImmutableSet<Size> = Size.entries.asIterable().toImmutableSet()
) : CommonParcelable