/*
 * Copyright (C) 2022 Adam Powell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit.backstack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

@Stable
public fun interface BackStackRecordLocalProvider<in R : BackStack.Record> {
  @Composable public fun providedValuesFor(record: R): ProvidedValues
}

@Stable
public fun interface ProvidedValues {
  @Composable public fun provideValues(): ImmutableList<ProvidedValue<*>>
}

internal class CompositeProvidedValues(private val list: List<ProvidedValues>) : ProvidedValues {
  @Composable
  override fun provideValues(): ImmutableList<ProvidedValue<*>> =
    buildList { list.forEach { addAll(key(it) { it.provideValues() }) } }.toImmutableList()
}

@Composable
public fun <R : BackStack.Record> providedValuesForBackStack(
  backStack: BackStack<R>,
  backStackLocalProviders: ImmutableList<BackStackRecordLocalProvider<R>> = persistentListOf(),
): ImmutableMap<R, ProvidedValues> =
  buildMap(backStack.size) {
      backStack.forEach { record ->
        key(record) {
          put(
            record,
            CompositeProvidedValues(
              backStackLocalProviders.map { key(it) { it.providedValuesFor(record) } }
            ),
          )
        }
      }
    }
    .toImmutableMap()
