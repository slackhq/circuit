package com.slack.circuit.star.petlist

import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.Size
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet

// TODO KMP this
@Parcelize
class Filters(
  @TypeParceler<ImmutableSet<Gender>, ImmutableSetParceler>
  val genders: ImmutableSet<Gender> = Gender.entries.asIterable().toImmutableSet(),
  @TypeParceler<ImmutableSet<Size>, ImmutableSetParceler>
  val sizes: ImmutableSet<Size> = Size.entries.asIterable().toImmutableSet()
) : Parcelable