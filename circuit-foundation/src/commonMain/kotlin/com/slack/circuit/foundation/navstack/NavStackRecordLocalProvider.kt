// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.navstack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import com.slack.circuit.runtime.navigation.NavStack

@Stable
public fun interface NavStackRecordLocalProvider<in R : NavStack.Record> {
  @Composable public fun providedValuesFor(record: R): ProvidedValues
}

@Stable
public fun interface ProvidedValues {
  @Composable public fun provideValues(): List<ProvidedValue<*>>
}

internal class CompositeProvidedValues(private val list: List<ProvidedValues>) : ProvidedValues {
  @Composable
  override fun provideValues(): List<ProvidedValue<*>> = buildList {
    list.forEach { addAll(key(it) { it.provideValues() }) }
  }
}

@Composable
public fun <R : NavStack.Record> providedValuesForNavStack(
  navStack: NavStack<R>,
  navStackLocalProviders: List<NavStackRecordLocalProvider<R>> = emptyList(),
): Map<R, ProvidedValues> =
  buildMap(navStack.size) {
    navStack.snapshot()?.forEach { record ->
      key(record) {
        put(
          record,
          CompositeProvidedValues(
            navStackLocalProviders.map { key(it) { it.providedValuesFor(record) } }
          ),
        )
      }
    }
  }
