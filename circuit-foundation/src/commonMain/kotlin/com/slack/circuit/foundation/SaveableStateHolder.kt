// Copyright (C) 2020 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withCompositionLocal

/**
 * This is a copy of [androidx.compose.runtime.saveable.SaveableStateHolder], tweaked so that
 * [SaveableStateProvider] returns a value.
 */
internal interface SaveableStateHolder {
  /**
   * Put your content associated with a [key] inside the [content]. This will automatically save all
   * the states defined with [rememberSaveable] before disposing the content and will restore the
   * states when you compose with this key again.
   *
   * @param key to be used for saving and restoring the states for the subtree. Note that on Android
   *   you can only use types which can be stored inside the Bundle.
   */
  @Composable fun <R> SaveableStateProvider(key: Any, content: @Composable () -> R): R

  /** Removes the saved state associated with the passed [key]. */
  fun removeState(key: Any)
}

/** Creates and remembers the instance of [SaveableStateHolder]. */
@Composable
internal fun rememberSaveableStateHolderWithReturn(): SaveableStateHolder =
  rememberSaveable(saver = SaveableStateHolderImpl.Saver) { SaveableStateHolderImpl() }
    .apply { parentSaveableStateRegistry = LocalSaveableStateRegistry.current }

private class SaveableStateHolderImpl(
  private val savedStates: MutableMap<Any, Map<String, List<Any?>>> = mutableMapOf()
) : SaveableStateHolder {
  private val registryHolders = mutableMapOf<Any, RegistryHolder>()
  var parentSaveableStateRegistry: SaveableStateRegistry? = null

  @Composable
  override fun <R> SaveableStateProvider(key: Any, content: @Composable () -> R): R {
    val registryHolder = remember {
      require(parentSaveableStateRegistry?.canBeSaved(key) ?: true) {
        "Type of the key $key is not supported. On Android you can only use types " +
          "which can be stored inside the Bundle."
      }
      RegistryHolder(key)
    }

    val result =
      withCompositionLocal(
        LocalSaveableStateRegistry provides registryHolder.registry,
        content = content,
      )

    DisposableEffect(Unit) {
      require(key !in registryHolders) { "Key $key was used multiple times " }
      savedStates -= key
      registryHolders[key] = registryHolder
      onDispose {
        registryHolder.saveTo(savedStates)
        registryHolders -= key
      }
    }

    return result
  }

  private fun saveAll(): MutableMap<Any, Map<String, List<Any?>>>? {
    val map = savedStates.toMutableMap()
    registryHolders.values.forEach { it.saveTo(map) }
    return map.ifEmpty { null }
  }

  override fun removeState(key: Any) {
    val registryHolder = registryHolders[key]
    if (registryHolder != null) {
      registryHolder.shouldSave = false
    } else {
      savedStates -= key
    }
  }

  inner class RegistryHolder(val key: Any) {
    var shouldSave = true
    val registry: SaveableStateRegistry =
      SaveableStateRegistry(savedStates[key]) {
        parentSaveableStateRegistry?.canBeSaved(it) ?: true
      }

    fun saveTo(map: MutableMap<Any, Map<String, List<Any?>>>) {
      if (shouldSave) {
        val savedData = registry.performSave()
        if (savedData.isEmpty()) {
          map -= key
        } else {
          map[key] = savedData
        }
      }
    }
  }

  companion object {
    val Saver: Saver<SaveableStateHolderImpl, *> =
      Saver(save = { it.saveAll() }, restore = { SaveableStateHolderImpl(it) })
  }
}
