// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.CancellationHandle
import androidx.compose.runtime.Composer
import androidx.compose.runtime.retain.RetainedValuesStore
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import org.junit.Test

class RetainedContentPresenceIndicatorTest {

  @Test
  fun onRemembered_recomposerShutdown_doesNotThrowCancellationException() {
    var onEnteredCalled = false
    var onExitCalled = false

    val store =
      Proxy.newProxyInstance(
        RetainedValuesStore::class.java.classLoader,
        arrayOf(RetainedValuesStore::class.java),
        InvocationHandler { _, method, _ ->
          when (method.name) {
            "onContentEnteredComposition" -> {
              onEnteredCalled = true
              null
            }
            "onContentExitComposition" -> {
              onExitCalled = true
              null
            }
            "hashCode" -> 1
            "equals" -> false
            "toString" -> "TestStore"
            else -> null
          }
        },
      ) as RetainedValuesStore

    val composer =
      Proxy.newProxyInstance(
        Composer::class.java.classLoader,
        arrayOf(Composer::class.java),
        InvocationHandler { _, method, _ ->
          when (method.name) {
            "scheduleFrameEndCallback" -> {
              throw CancellationException(
                "Recomposer shutdown; frame clock awaiter will never resume"
              )
            }
            "hashCode" -> 1
            "equals" -> false
            "toString" -> "TestComposer"
            else -> null
          }
        },
      ) as Composer

    val indicator = RetainedContentPresenceIndicator(store, composer)

    // Should not throw CancellationException (regression test for #2913)
    indicator.onRemembered()

    assertFalse(onEnteredCalled)

    // onForgotten should be safe and not exit composition
    indicator.onForgotten()
    assertFalse(onExitCalled)

    // onAbandoned should also be safe
    indicator.onAbandoned()
  }

  @Test
  fun onRemembered_normal_entersCompositionAndExitsOnForgotten() {
    var onEnteredCalled = false
    var onExitCalled = false
    var callback: (() -> Unit)? = null

    val store =
      Proxy.newProxyInstance(
        RetainedValuesStore::class.java.classLoader,
        arrayOf(RetainedValuesStore::class.java),
        InvocationHandler { _, method, _ ->
          when (method.name) {
            "onContentEnteredComposition" -> {
              onEnteredCalled = true
              null
            }
            "onContentExitComposition" -> {
              onExitCalled = true
              null
            }
            "hashCode" -> 1
            "equals" -> false
            "toString" -> "TestStore"
            else -> null
          }
        },
      ) as RetainedValuesStore

    val cancellationHandle = CancellationHandle {}

    val composer =
      Proxy.newProxyInstance(
        Composer::class.java.classLoader,
        arrayOf(Composer::class.java),
        InvocationHandler { _, method, args ->
          when (method.name) {
            "scheduleFrameEndCallback" -> {
              @Suppress("UNCHECKED_CAST")
              callback = args[0] as () -> Unit
              cancellationHandle
            }
            "hashCode" -> 1
            "equals" -> false
            "toString" -> "TestComposer"
            else -> null
          }
        },
      ) as Composer

    val indicator = RetainedContentPresenceIndicator(store, composer)

    indicator.onRemembered()
    callback?.invoke()

    assertTrue(onEnteredCalled)
    assertFalse(onExitCalled)

    indicator.onForgotten()
    assertTrue(onExitCalled)
  }
}
