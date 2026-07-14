// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.retain.RetainObserver
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RetainedStoreRecipeTest {

  private class ObservingValue : RetainObserver {
    val events = mutableListOf<String>()

    override fun onRetained() {
      events += "retained"
    }

    override fun onEnteredComposition() {
      events += "entered"
    }

    override fun onExitedComposition() {
      events += "exited"
    }

    override fun onRetired() {
      events += "retired"
    }

    override fun onUnused() {
      events += "unused"
    }
  }

  private class CloseableValue : AutoCloseable {
    var closed = false

    override fun close() {
      closed = true
    }
  }

  @Test
  fun getOrCreateReturnsSameValueForSameKey() {
    val store = RetainedStore<String, Any>()
    val first = store.getOrCreate("a") { Any() }
    val second = store.getOrCreate("a") { Any() }
    assertThat(second).isSameInstanceAs(first)
    assertThat(store.getOrCreate("b") { Any() }).isNotSameInstanceAs(first)
  }

  @Test
  fun getReturnsPresentValuesOnly() {
    val store = RetainedStore<String, Any>()
    assertThat(store["a"]).isNull()
    val value = store.getOrCreate("a") { Any() }
    assertThat(store["a"]).isSameInstanceAs(value)
    assertThat(store.keys).containsExactly("a")
  }

  @Test
  fun createdValueReceivesRetainedAndCompositionState() {
    val store = RetainedStore<String, ObservingValue>()
    val beforeEnter = store.getOrCreate("before") { ObservingValue() }
    assertThat(beforeEnter.events).containsExactly("retained").inOrder()

    store.onEnteredComposition()
    assertThat(beforeEnter.events).containsExactly("retained", "entered").inOrder()

    val afterEnter = store.getOrCreate("after") { ObservingValue() }
    assertThat(afterEnter.events).containsExactly("retained", "entered").inOrder()

    store.onExitedComposition()
    assertThat(afterEnter.events).containsExactly("retained", "entered", "exited").inOrder()
  }

  @Test
  fun removeRetiresValue() {
    val store = RetainedStore<String, ObservingValue>()
    store.onEnteredComposition()
    val value = store.getOrCreate("a") { ObservingValue() }
    val removed = store.remove("a")
    assertThat(removed).isSameInstanceAs(value)
    assertThat(value.events).containsExactly("retained", "entered", "exited", "retired").inOrder()
    assertThat(store["a"]).isNull()
    assertThat(store.remove("a")).isNull()
  }

  @Test
  fun storeRetirementRetiresAllValues() {
    val store = RetainedStore<String, ObservingValue>()
    val a = store.getOrCreate("a") { ObservingValue() }
    val b = store.getOrCreate("b") { ObservingValue() }
    store.onRetired()
    assertThat(a.events).containsExactly("retained", "retired").inOrder()
    assertThat(b.events).containsExactly("retained", "retired").inOrder()
    assertThat(store.keys).isEmpty()
  }

  @Test
  fun closeableValuesAreClosedOnRetirement() {
    val store = RetainedStore<String, CloseableValue>()
    val a = store.getOrCreate("a") { CloseableValue() }
    val b = store.getOrCreate("b") { CloseableValue() }
    store.remove("a")
    assertThat(a.closed).isTrue()
    assertThat(b.closed).isFalse()
    store.onRetired()
    assertThat(b.closed).isTrue()
  }

  @Test
  fun changedInputsRecreateValue() {
    val store = RetainedStore<String, ObservingValue>()
    val first = store.getOrCreate("a", 1) { ObservingValue() }
    val same = store.getOrCreate("a", 1) { ObservingValue() }
    assertThat(same).isSameInstanceAs(first)

    val recreated = store.getOrCreate("a", 2) { ObservingValue() }
    assertThat(recreated).isNotSameInstanceAs(first)
    assertThat(first.events).containsExactly("retained", "retired").inOrder()
    assertThat(recreated.events).containsExactly("retained").inOrder()
  }

  @Test
  fun noInputsPreservesExistingValueCreatedWithInputs() {
    val store = RetainedStore<String, Any>()
    val first = store.getOrCreate("a", 1) { Any() }
    val second = store.getOrCreate("a") { Any() }
    assertThat(second).isSameInstanceAs(first)
  }

  @Test
  fun clearRetiresEverything() {
    val store = RetainedStore<String, ObservingValue>()
    val a = store.getOrCreate("a") { ObservingValue() }
    store.clear()
    assertThat(a.events).containsExactly("retained", "retired").inOrder()
    assertThat(store.keys).isEmpty()
  }

  @Test
  fun releaseEvictsUnreferencedEntryWhileInComposition() {
    val store = RetainedStore<String, ObservingValue>()
    store.onEnteredComposition()
    val value = store.acquire("a") { ObservingValue() }
    store.release("a", value)
    assertThat(store["a"]).isNull()
    assertThat(value.events).containsExactly("retained", "entered", "exited", "retired").inOrder()
  }

  @Test
  fun releaseDuringTeardownKeepsEntryForRestoration() {
    val store = RetainedStore<String, ObservingValue>()
    store.onEnteredComposition()
    val value = store.acquire("a") { ObservingValue() }

    // Config-change-style teardown: store exits composition, then composition disposal releases.
    store.onExitedComposition()
    store.release("a", value)
    assertThat(store["a"]).isSameInstanceAs(value)

    // Restoration re-acquires before the store re-enters composition (frame end).
    val reacquired = store.acquire("a") { ObservingValue() }
    assertThat(reacquired).isSameInstanceAs(value)
    store.onEnteredComposition()
    assertThat(store["a"]).isSameInstanceAs(value)
  }

  @Test
  fun unreferencedEntriesEvictedWhenStoreReentersComposition() {
    val store = RetainedStore<String, ObservingValue>()
    store.onEnteredComposition()
    val kept = store.acquire("kept") { ObservingValue() }
    val dropped = store.acquire("dropped") { ObservingValue() }

    store.onExitedComposition()
    store.release("kept", kept)
    store.release("dropped", dropped)

    // Only "kept" is re-acquired by the restored composition.
    store.acquire("kept") { ObservingValue() }
    store.onEnteredComposition()

    assertThat(store["kept"]).isSameInstanceAs(kept)
    assertThat(store["dropped"]).isNull()
    assertThat(dropped.events).containsExactly("retained", "entered", "exited", "retired").inOrder()
  }

  @Test
  fun staleReleaseAfterInputRecreationIsIgnored() {
    val store = RetainedStore<String, ObservingValue>()
    store.onEnteredComposition()
    val old = store.acquire("a", 1) { ObservingValue() }
    // Inputs change: a new entry is acquired before the old reference is released, matching
    // composition ordering (remember re-runs in composition, disposal happens in apply).
    val new = store.acquire("a", 2) { ObservingValue() }
    store.release("a", old)
    assertThat(store["a"]).isSameInstanceAs(new)
    assertThat(old.events).containsExactly("retained", "entered", "exited", "retired").inOrder()
  }
}
