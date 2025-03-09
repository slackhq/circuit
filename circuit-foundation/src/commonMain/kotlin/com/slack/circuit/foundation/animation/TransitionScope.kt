package com.slack.circuit.foundation.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import com.slack.circuit.backstack.NavArgument

public typealias TransitionScope<T, S> = AnimatedContentTransitionScope<AnimatedContentState<T, S>>

public typealias AnyTransitionScope =
  AnimatedContentTransitionScope<AnimatedContentState<NavArgument, AnimatedNavState<NavArgument>>>
