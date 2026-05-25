// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Registry for SubCircuit presenter and UI factories.
 *
 * SubCircuit provides a way to resolve presenters and UIs for [SubScreen]s without depending on
 * Circuit's navigation infrastructure. This is useful for composable components that need to render
 * child screens but delegate navigation and other cross-cutting concerns to a parent component.
 *
 * Create a [SubCircuit] instance using the [Builder]:
 * ```kotlin
 * val subCircuit = SubCircuit.builder()
 *   .addPresenterFactories(presenterFactories)
 *   .addUiFactories(uiFactories)
 *   .build()
 * ```
 *
 * @see SubCircuitContent
 * @see LocalSubCircuit
 */
@Immutable
public class SubCircuit
private constructor(
  private val presenterFactories: Set<SubPresenterFactory>,
  private val uiFactories: Set<SubUiFactory>,
) {
  /**
   * Looks up a [SubPresenter] for the given [screen].
   *
   * @param screen The screen to find a presenter for.
   * @return The presenter if found, null otherwise.
   */
  public fun presenter(screen: SubScreen<*>): SubPresenter<*, *>? =
    presenterFactories.firstNotNullOfOrNull {
      it.create(screen)
    }

  /**
   * Looks up a [SubUi] for the given [screen].
   *
   * @param screen The screen to find a UI for.
   * @return The UI if found, null otherwise.
   */
  public fun ui(screen: SubScreen<*>): SubUi<*>? = uiFactories.firstNotNullOfOrNull {
    it.create(screen)
  }

  /** Builder for creating [SubCircuit] instances. */
  public class Builder {
    private val presenterFactories = mutableSetOf<SubPresenterFactory>()
    private val uiFactories = mutableSetOf<SubUiFactory>()

    /** Adds presenter factories to the registry. */
    public fun addPresenterFactories(factories: Set<SubPresenterFactory>): Builder = apply {
      presenterFactories.addAll(factories)
    }

    /** Adds a single presenter factory to the registry. */
    public fun addPresenterFactory(factory: SubPresenterFactory): Builder = apply {
      presenterFactories.add(factory)
    }

    /** Adds UI factories to the registry. */
    public fun addUiFactories(factories: Set<SubUiFactory>): Builder = apply {
      uiFactories.addAll(factories)
    }

    /** Adds a single UI factory to the registry. */
    public fun addUiFactory(factory: SubUiFactory): Builder = apply { uiFactories.add(factory) }

    /** Builds the [SubCircuit] instance. */
    public fun build(): SubCircuit = SubCircuit(presenterFactories.toSet(), uiFactories.toSet())
  }

  public companion object {
    /** Creates a new [Builder] for constructing a [SubCircuit]. */
    public fun builder(): Builder = Builder()
  }
}

/**
 * CompositionLocal for providing a [SubCircuit] instance to [SubCircuitContent].
 *
 * This should be provided at the app level to make SubCircuit available throughout the UI tree.
 */
public val LocalSubCircuit: ProvidableCompositionLocal<SubCircuit?> = staticCompositionLocalOf {
  null
}
