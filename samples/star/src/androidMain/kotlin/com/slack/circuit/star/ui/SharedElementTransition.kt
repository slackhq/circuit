package com.slack.circuit.star.ui

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedElementTransitionScope(content: @Composable SharedTransitionScope.() -> Unit) {
  LocalSharedTransitionScope.current.content()
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedElementTransitionLayout(
  modifier: Modifier = Modifier,
  content: @Composable SharedTransitionScope.() -> Unit,
) {
  SharedTransitionLayout(modifier = modifier) {
    CompositionLocalProvider(LocalSharedTransitionScope provides this) { content() }
  }
}

@SuppressLint("ComposeCompositionLocalUsage")
@OptIn(ExperimentalSharedTransitionApi::class)
private val LocalSharedTransitionScope =
  compositionLocalOf<SharedTransitionScope> { error("Not in a SharedElementTransitionLayout") }
