package com.slack.circuit.backstack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key

fun interface BackStackRecordLocalProvider<in R : BackStack.Record> {
  @Composable fun providedValuesFor(record: R): ProvidedValues
}

fun interface ProvidedValues {
  @Composable fun provideValues(): List<ProvidedValue<*>>
}

class CompositeProvidedValues(private val list: List<ProvidedValues>) : ProvidedValues {
  @Composable
  override fun provideValues(): List<ProvidedValue<*>> = buildList {
    list.forEach { addAll(key(it) { it.provideValues() }) }
  }
}

@Composable
fun <R : BackStack.Record> providedValuesForBackStack(
  backStack: BackStack<R>,
  stackLocalProviders: List<BackStackRecordLocalProvider<R>> = emptyList(),
  includeDefaults: Boolean = true,
): Map<R, ProvidedValues> = buildMap {
  backStack.forEach { record ->
    key(record) {
      put(
        record,
        CompositeProvidedValues(
          buildList {
            if (includeDefaults) {
              LocalBackStackRecordLocalProviders.current.forEach {
                add(key(it) { it.providedValuesFor(record) })
              }
            }
            stackLocalProviders.forEach { add(key(it) { it.providedValuesFor(record) }) }
          }
        )
      )
    }
  }
}

@Suppress("RemoveExplicitTypeArguments")
val LocalBackStackRecordLocalProviders =
  compositionLocalOf<List<BackStackRecordLocalProvider<BackStack.Record>>> {
    listOf(SaveableStateRegistryBackStackRecordLocalProvider, ViewModelBackStackRecordLocalProvider)
  }
