package com.slack.circuitx.gesturenavigation

import com.slack.circuit.foundation.AnimatedNavDecorator

public actual fun GestureNavigationDecorationFactory(
  fallback: AnimatedNavDecorator.Factory,
  onBackInvoked: () -> Unit,
): AnimatedNavDecorator.Factory {
  TODO("Rebuild CupertinoGestureNavigationDecoration as a AnimatedNavDecorator.Factory")
  //    CupertinoGestureNavigationDecoration(onBackInvoked = onBackInvoked)
}
