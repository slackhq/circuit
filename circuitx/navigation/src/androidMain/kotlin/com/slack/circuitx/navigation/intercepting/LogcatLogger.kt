package com.slack.circuitx.navigation.intercepting

import android.util.Log

/** A [NavigationLogger] that logs to Logcat at the [Log.INFO] level. */
public object LogcatLogger : NavigationLogger {
  override fun log(message: String) {
    Log.i("Circuit Navigation", message)
  }
}
