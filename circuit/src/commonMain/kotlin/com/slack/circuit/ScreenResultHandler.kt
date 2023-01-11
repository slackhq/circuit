package com.slack.circuit

public fun interface ScreenResultHandler {
  public operator fun invoke(result: ScreenResult?)

  public object NoOp : ScreenResultHandler {
    override operator fun invoke(result: ScreenResult?) { }
  }
}