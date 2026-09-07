// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.retain.ManagedRetainedValuesStore
import androidx.compose.runtime.retain.RetainObserver
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCircuitRetainedApi::class)
class RetainedValuesStoreOwnerTest {
  private val owners = mutableListOf<RetainedValuesStoreOwner>()

  @After
  fun tearDown() {
    owners.forEach(RetainedValuesStoreOwner::dispose)
  }

  @Test
  fun successfulLeaseIsReusedAtTheSameLocation() {
    val owner = newOwner()
    val first = owner.acquire(location = 1)
    first.onRemembered()
    val observer = first.store.storeObserver()

    first.onForgotten()

    val second = owner.acquire(location = 1)
    assertSame(first.store, second.store)
    assertEquals(0, observer.retiredCount)
  }

  @Test
  fun abandonedInitialLeaseIsDisposed() {
    val owner = newOwner()
    val first = owner.acquire(location = 1)
    val observer = first.store.storeObserver()

    first.onAbandoned()

    val second = owner.acquire(location = 1)
    assertNotSame(first.store, second.store)
    assertEquals(1, observer.retiredCount)
  }

  @Test
  fun abandoningEstablishedLeasePreservesItsStore() {
    val owner = newOwner()
    val first = owner.acquire(location = 1)
    first.onRemembered()
    first.onForgotten()
    val observer = first.store.storeObserver()

    val second = owner.acquire(location = 1)
    second.onAbandoned()

    val third = owner.acquire(location = 1)
    assertSame(first.store, third.store)
    assertEquals(0, observer.retiredCount)
  }

  @Test
  fun activeEntriesAtTheSameLocationDoNotShareAStore() {
    val owner = newOwner()
    val first = owner.acquire(location = 1)
    first.onRemembered()

    val second = owner.acquire(location = 1)

    assertNotSame(first.store, second.store)
  }

  @Test
  fun releasedEntriesAtTheSameLocationAreReusedInAcquisitionOrder() {
    val owner = newOwner()
    val first = owner.acquire(location = 1)
    first.onRemembered()
    val firstObserver = first.store.storeObserver()
    val second = owner.acquire(location = 1)
    second.onRemembered()
    val secondObserver = second.store.storeObserver()

    second.onForgotten()
    first.onForgotten()

    val restoredFirst = owner.acquire(location = 1)
    val restoredSecond = owner.acquire(location = 1)
    assertSame(first.store, restoredFirst.store)
    assertSame(second.store, restoredSecond.store)
    assertEquals(0, firstObserver.retiredCount)
    assertEquals(0, secondObserver.retiredCount)
  }

  @Test
  fun abandonedOverlappingLeasePreservesEstablishedStore() {
    val owner = newOwner()
    val first = owner.acquire(location = 1)
    first.onRemembered()
    val second = owner.acquire(location = 1)
    val secondObserver = second.store.storeObserver()

    second.onAbandoned()
    first.onForgotten()

    assertSame(first.store, owner.acquire(location = 1).store)
    assertEquals(1, secondObserver.retiredCount)
  }

  @Test
  fun abandonedOverlapPreservesEstablishedStoreAfterItReleases() {
    val owner = newOwner()
    val first = owner.acquire(location = 1)
    first.onRemembered()
    val second = owner.acquire(location = 1)
    first.onForgotten()

    second.onAbandoned()

    assertSame(first.store, owner.acquire(location = 1).store)
  }

  @Test
  fun candidateIsRemovedBeforeItsStoreIsDisposed() {
    val owner = newOwner()
    val first = owner.acquire(location = 1)
    first.onRemembered()
    var reentrantLease: RetainedValuesStoreLease? = null
    val second = owner.acquire(location = 1)
    second.store.storeObserver { reentrantLease = owner.acquire(location = 1) }

    second.onAbandoned()

    val acquiredDuringRetirement = requireNotNull(reentrantLease)
    assertNotSame(first.store, acquiredDuringRetirement.store)
    assertNotSame(second.store, acquiredDuringRetirement.store)
    acquiredDuringRetirement.onAbandoned()
    first.onForgotten()
  }

  @Test
  fun differentLocationsDoNotShareAStore() {
    val owner = newOwner()
    val first = owner.acquire(location = 1)
    first.onRemembered()
    first.onForgotten()

    val second = owner.acquire(location = 2)
    assertNotSame(first.store, second.store)
  }

  @Test
  fun terminalLeaseCallbacksAreIdempotent() {
    val owner = newOwner()
    val first = owner.acquire(location = 1)
    first.onRemembered()
    first.onForgotten()
    val replacement = owner.acquire(location = 1)

    first.onForgotten()
    first.onAbandoned()

    val overlapping = owner.acquire(location = 1)
    assertNotSame(replacement.store, overlapping.store)
  }

  @Test
  fun disposeRetiresEveryStoreExactlyOnce() {
    val owner = newOwner()
    val available = owner.acquire(location = 1)
    available.onRemembered()
    val availableObserver = available.store.storeObserver()
    available.onForgotten()
    val active = owner.acquire(location = 2)
    active.onRemembered()
    val activeObserver = active.store.storeObserver()
    val provisional = owner.acquire(location = 2)
    val provisionalObserver = provisional.store.storeObserver()

    owner.dispose()
    owner.dispose()
    active.onForgotten()
    provisional.onAbandoned()

    assertEquals(1, availableObserver.retiredCount)
    assertEquals(1, activeObserver.retiredCount)
    assertEquals(1, provisionalObserver.retiredCount)
  }

  @Test
  fun disposedOwnerRejectsNewLeases() {
    val owner = newOwner()
    owner.dispose()

    assertFailsWith<IllegalStateException> { owner.acquire(location = 1) }
  }

  private class CountingRetainObserver(private val onRetiredCallback: () -> Unit = {}) :
    RetainObserver {
    var retiredCount = 0

    override fun onRetained() = Unit

    override fun onEnteredComposition() = Unit

    override fun onExitedComposition() = Unit

    override fun onRetired() {
      retiredCount++
      onRetiredCallback()
    }

    override fun onUnused() = Unit
  }

  private fun ManagedRetainedValuesStore.storeObserver(
    onRetired: () -> Unit = {}
  ): CountingRetainObserver =
    CountingRetainObserver(onRetired).also { observer -> saveExitingValue("value", observer) }

  private fun newOwner(): RetainedValuesStoreOwner = RetainedValuesStoreOwner().also(owners::add)
}
