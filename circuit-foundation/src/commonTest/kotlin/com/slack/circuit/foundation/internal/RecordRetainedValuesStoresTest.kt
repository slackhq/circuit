// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.internal

import androidx.compose.runtime.retain.RetainObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class RecordRetainedValuesStoresTest {

  @Test
  fun duplicateKeysAreConsumedInLifoOrder() {
    val store = RecordRetainedValuesStores().storeFor("record")
    val first = Any()
    val second = Any()

    store.saveExitingValue("value", first)
    store.saveExitingValue("value", second)

    assertSame(second, store.consumeExitedValueOrDefault("value", null))
    assertSame(first, store.consumeExitedValueOrDefault("value", null))
    assertSame(Missing, store.consumeExitedValueOrDefault("value", Missing))
  }

  @Test
  fun enteringCompositionRetiresUnclaimedValues() {
    val store = RecordRetainedValuesStores().storeFor("record")
    val claimed = TrackingRetainObserver()
    val unclaimed = TrackingRetainObserver()
    store.saveExitingValue("claimed", claimed)
    store.saveExitingValue("unclaimed", unclaimed)

    assertSame(claimed, store.consumeExitedValueOrDefault("claimed", null))
    store.onContentEnteredComposition()

    assertEquals(0, claimed.retiredCount)
    assertEquals(1, unclaimed.retiredCount)
  }

  @Test
  fun additionalCompositionDefersCleanupUntilAllInstallationsExit() {
    val store = RecordRetainedValuesStores().storeFor("record")
    store.onContentEnteredComposition()
    store.onContentEnteredComposition()
    store.onContentExitComposition()
    val value = TrackingRetainObserver()
    store.saveExitingValue("value", value)

    store.onContentEnteredComposition()

    assertEquals(0, value.retiredCount)
    store.onContentExitComposition()
    store.onContentExitComposition()
    store.onContentEnteredComposition()
    assertEquals(1, value.retiredCount)
  }

  @Test
  fun clearRetiresSavedValuesExactlyOnce() {
    val stores = RecordRetainedValuesStores()
    val store = stores.storeFor("record")
    val value = TrackingRetainObserver()
    store.saveExitingValue("value", value)

    stores.clear("record")
    stores.clear("record")

    assertEquals(1, value.retiredCount)
    assertSame(Missing, store.consumeExitedValueOrDefault("value", Missing))
  }

  @Test
  fun saveAfterClearRetiresValueInsteadOfKeepingIt() {
    val stores = RecordRetainedValuesStores()
    val store = stores.storeFor("record")
    stores.clear("record")
    val value = TrackingRetainObserver()

    store.onContentExitComposition()
    store.onContentEnteredComposition()
    store.saveExitingValue("value", value)

    assertEquals(1, value.retiredCount)
    assertSame(Missing, store.consumeExitedValueOrDefault("value", Missing))
  }

  @Test
  fun clearAllowsFreshStoreForSameRecord() {
    val stores = RecordRetainedValuesStores()
    val oldStore = stores.storeFor("record")
    stores.clear("record")

    val newStore = stores.storeFor("record")
    val value = Any()
    newStore.saveExitingValue("value", value)

    assertNotSame(oldStore, newStore)
    assertSame(value, newStore.consumeExitedValueOrDefault("value", null))
  }

  @Test
  fun disposeRetiresValuesAcrossRecordsExactlyOnce() {
    val stores = RecordRetainedValuesStores()
    val first = TrackingRetainObserver()
    val second = TrackingRetainObserver()
    stores.storeFor("first").saveExitingValue("value", first)
    stores.storeFor("second").saveExitingValue("value", second)

    stores.dispose()
    stores.dispose()

    assertEquals(1, first.retiredCount)
    assertEquals(1, second.retiredCount)
  }

  @Test
  fun saveAfterDisposeRetiresValueInsteadOfKeepingIt() {
    val stores = RecordRetainedValuesStores()
    val store = stores.storeFor("record")
    stores.dispose()
    val value = TrackingRetainObserver()

    store.saveExitingValue("value", value)

    assertEquals(1, value.retiredCount)
    assertSame(Missing, store.consumeExitedValueOrDefault("value", Missing))
  }

  @Test
  fun storeRequestedAfterDisposeIsForgetful() {
    val stores = RecordRetainedValuesStores()
    stores.dispose()
    val store = stores.storeFor("record")
    val value = TrackingRetainObserver()

    store.saveExitingValue("value", value)

    assertEquals(1, value.retiredCount)
    assertSame(Missing, store.consumeExitedValueOrDefault("value", Missing))
  }

  private class TrackingRetainObserver : RetainObserver {
    var retiredCount = 0

    override fun onRetained() {}

    override fun onEnteredComposition() {}

    override fun onExitedComposition() {}

    override fun onRetired() {
      retiredCount++
    }

    override fun onUnused() {}
  }

  private companion object {
    val Missing = Any()
  }
}
