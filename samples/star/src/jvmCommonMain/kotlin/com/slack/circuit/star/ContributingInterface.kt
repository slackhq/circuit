package com.slack.circuit.star

import coil3.ImageLoader
import com.slack.circuit.star.di.AppScope
import com.squareup.anvil.annotations.ContributesTo

// TODO https://youtrack.jetbrains.com/issue/KT-67490
//@ContributesTo(AppScope::class)
interface ContributingInterface {
  fun imageLoader(): ImageLoader
}