// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.internal

internal inline fun <T, R> Iterable<T>.mapToImmutableList(transform: (T) -> R): List<R> {
  val size = if (this is Collection<*>) size else -1
  return when (size) {
    0 -> emptyList()
    1 -> {
      // singletonList on JVM
      listOf(transform(first()))
    }
    else -> {
      buildList {
        for (element in this@mapToImmutableList) {
          add(transform(element))
        }
      }
    }
  }
}

internal inline fun <T, R> Iterable<T>.mapToImmutableSet(transform: (T) -> R): Set<R> {
  val size = if (this is Collection<*>) size else -1
  return when (size) {
    0 -> emptySet()
    1 -> {
      // Collections.singleton on JVM
      setOf(transform(first()))
    }
    else -> {
      buildSet {
        for (element in this@mapToImmutableSet) {
          add(transform(element))
        }
      }
    }
  }
}

internal fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V> {
  return when (size) {
    0 -> emptyMap()
    1 -> {
      val (k, v) = entries.first()
      // singletonMap on the JVM
      mapOf(k to v)
    }
    else ->
      buildMap {
        for ((key, value) in this@toImmutableMap) {
          put(key, value)
        }
      }
  }
}
