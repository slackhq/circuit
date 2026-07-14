// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName

/**
 * The flavor of Circuit the processor is generating for: the runtime types and naming rules that
 * differ between `@CircuitInject` ([Circuit]) and `@SubCircuitInject` ([SubCircuit]). Everything
 * else — DI-mode wiring, provider hoisting, qualifier/origin propagation, the `when (screen)`
 * skeleton — is shared and lives in the processor.
 *
 * A single [CircuitSymbolProcessor][com.slack.circuit.codegen.CircuitSymbolProcessorProvider] scans
 * both annotations and drives generation with the matching target per element. Pairs with
 * [CodegenMode], which selects the DI framework.
 */
internal sealed interface CodegenTarget {
  /** The inject annotation this target scans and reads the `screen`/`scope` arguments from. */
  val injectAnnotation: ClassName
  /** Simple name of [injectAnnotation], used in diagnostics. */
  val annotationSimpleName: String

  /** Human-readable noun for the UI type, used in diagnostics (e.g. `UI`, `SubUi`). */
  val uiNoun: String
  /** Human-readable noun for the presenter type, used in diagnostics. */
  val presenterNoun: String

  /** The UI factory interface the generated UI factory implements. */
  val uiFactory: ClassName
  /** The presenter factory interface the generated presenter factory implements. */
  val presenterFactory: ClassName

  /** Fallback UI-state type used for UI functions that omit a state parameter. */
  val defaultUiState: ClassName

  /**
   * Whether this target provides assisted arguments (screen/navigator/context) to top-level
   * functions. Circuit does; SubCircuit functions only receive state and modifier.
   */
  val providesAssistedParamsToFunctions: Boolean

  /** The UI-state [KSType] used to detect a function's state parameter. */
  fun uiState(symbols: CircuitSymbols): KSType

  /** Maps a supertype's qualified name to its [FactoryType], or null if it isn't a UI/presenter. */
  fun factoryTypeForSuperType(qualifiedName: String): FactoryType?

  /** The [FactoryType] a top-level function produces. */
  fun functionFactoryType(functionName: String): FactoryType

  /** Whether [type] is (or extends) this target's screen type. */
  fun isScreenType(type: KSType, symbols: CircuitSymbols): Boolean

  /** Whether [type] is this target's navigator type. Only Circuit provides a navigator. */
  fun isNavigatorType(type: KSType, symbols: CircuitSymbols): Boolean = false

  /** Whether [type] is this target's context type. Only Circuit provides a context. */
  fun isContextType(type: KSType, symbols: CircuitSymbols): Boolean = false

  /**
   * Whether a top-level function parameter of [type] is framework-provided (and so skipped from
   * injection) rather than an injected dependency.
   */
  fun isProvidedFunctionParameter(
    type: KSType,
    factoryType: FactoryType,
    symbols: CircuitSymbols,
  ): Boolean

  /**
   * The leading token of the UI-function wrapper, applied as `<prefix><State> { state, modifier ->
   * fn(...) }`. Circuit uses the `ui` member function; SubCircuit uses the `SubUi` SAM type.
   */
  fun uiFunctionWrapperPrefix(): CodeBlock

  /** The parameters of the generated factory's `create` function for the given [factoryType]. */
  fun createParams(factoryType: FactoryType): List<ParameterSpec>

  /** The (nullable) return type of the generated factory's `create` function. */
  fun createReturn(factoryType: FactoryType): TypeName

  /** The factory interface the generated class implements for the given [factoryType]. */
  fun factorySuperinterface(factoryType: FactoryType): ClassName =
    if (factoryType == FactoryType.UI) uiFactory else presenterFactory

  /**
   * The base name of the generated factory class (before capitalization + suffix), for the
   * class-based instantiation path.
   */
  fun classBaseName(
    annotatedDeclaration: KSClassDeclaration,
    targetClass: KSClassDeclaration,
    isAssisted: Boolean,
    isKotlinInjectAnvil: Boolean,
  ): String

  /** The package the generated factory class is emitted into, for the class-based path. */
  fun classPackageName(
    annotatedDeclaration: KSClassDeclaration,
    targetClass: KSClassDeclaration,
  ): String

  /** The suffix appended to the base name to form the generated factory class name. */
  fun factorySuffix(factoryType: FactoryType): String

  /** Diagnostic emitted when an annotated class isn't injectable. */
  fun notInjectableError(): String

  data object Circuit : CodegenTarget {
    override val injectAnnotation = CircuitNames.CIRCUIT_INJECT_ANNOTATION
    override val annotationSimpleName = injectAnnotation.simpleName
    override val uiNoun = "UI"
    override val presenterNoun = "Presenter"
    override val uiFactory = CircuitNames.CIRCUIT_UI_FACTORY
    override val presenterFactory = CircuitNames.CIRCUIT_PRESENTER_FACTORY
    override val defaultUiState = CircuitNames.CIRCUIT_UI_STATE
    override val providesAssistedParamsToFunctions = true

    override fun uiState(symbols: CircuitSymbols) = symbols.circuitUiState

    override fun factoryTypeForSuperType(qualifiedName: String): FactoryType? =
      when (qualifiedName) {
        CircuitNames.CIRCUIT_UI.canonicalName -> FactoryType.UI
        CircuitNames.CIRCUIT_PRESENTER.canonicalName -> FactoryType.PRESENTER
        else -> null
      }

    override fun functionFactoryType(functionName: String): FactoryType =
      if (functionName.endsWith("Presenter")) FactoryType.PRESENTER else FactoryType.UI

    override fun isScreenType(type: KSType, symbols: CircuitSymbols): Boolean =
      symbols.screen.isAssignableFrom(type)

    override fun isNavigatorType(type: KSType, symbols: CircuitSymbols): Boolean =
      symbols.navigator.isAssignableFrom(type)

    override fun isContextType(type: KSType, symbols: CircuitSymbols): Boolean =
      symbols.circuitContext.isAssignableFrom(type)

    override fun isProvidedFunctionParameter(
      type: KSType,
      factoryType: FactoryType,
      symbols: CircuitSymbols,
    ): Boolean {
      if (isScreenType(type, symbols) || isContextType(type, symbols)) return true
      return when (factoryType) {
        FactoryType.PRESENTER -> isNavigatorType(type, symbols)
        FactoryType.UI ->
          symbols.circuitUiState.isAssignableFrom(type) || symbols.modifier.isAssignableFrom(type)
      }
    }

    override fun uiFunctionWrapperPrefix(): CodeBlock =
      CodeBlock.of("%M", MemberName(CircuitNames.CIRCUIT_RUNTIME_UI_PACKAGE, "ui"))

    override fun createParams(factoryType: FactoryType): List<ParameterSpec> = buildList {
      add(ParameterSpec.builder("screen", CircuitNames.SCREEN).build())
      if (factoryType == FactoryType.PRESENTER) {
        add(ParameterSpec.builder("navigator", CircuitNames.NAVIGATOR).build())
      }
      add(ParameterSpec.builder("context", CircuitNames.CIRCUIT_CONTEXT).build())
    }

    override fun createReturn(factoryType: FactoryType): TypeName {
      val base =
        if (factoryType == FactoryType.UI) CircuitNames.CIRCUIT_UI
        else CircuitNames.CIRCUIT_PRESENTER
      return base.parameterizedBy(STAR).copy(nullable = true)
    }

    override fun classBaseName(
      annotatedDeclaration: KSClassDeclaration,
      targetClass: KSClassDeclaration,
      isAssisted: Boolean,
      isKotlinInjectAnvil: Boolean,
    ): String = targetClass.simpleName.getShortName()

    override fun classPackageName(
      annotatedDeclaration: KSClassDeclaration,
      targetClass: KSClassDeclaration,
    ): String = targetClass.packageName.asString()

    override fun factorySuffix(factoryType: FactoryType): String = CircuitNames.FACTORY

    override fun notInjectableError(): String =
      "@$annotationSimpleName-annotated classes must be injectable: annotate the class or a " +
        "constructor with @Inject."
  }

  data object SubCircuit : CodegenTarget {
    override val injectAnnotation = CircuitNames.SUB_CIRCUIT_INJECT_ANNOTATION
    override val annotationSimpleName = injectAnnotation.simpleName
    override val uiNoun = "SubUi"
    override val presenterNoun = "SubPresenter"
    override val uiFactory = CircuitNames.SUB_UI_FACTORY
    override val presenterFactory = CircuitNames.SUB_PRESENTER_FACTORY
    override val defaultUiState = CircuitNames.SUB_CIRCUIT_UI_STATE
    override val providesAssistedParamsToFunctions = false

    override fun uiState(symbols: CircuitSymbols) = symbols.subCircuitUiState

    override fun factoryTypeForSuperType(qualifiedName: String): FactoryType? =
      when (qualifiedName) {
        CircuitNames.SUB_UI.canonicalName -> FactoryType.UI
        CircuitNames.SUB_PRESENTER.canonicalName -> FactoryType.PRESENTER
        else -> null
      }

    // SubCircuit has no `presenterOf` equivalent and SubPresenter is not a fun interface, so
    // function presenters are unsupported: annotated functions are always UI.
    override fun functionFactoryType(functionName: String): FactoryType = FactoryType.UI

    // SubScreen is generic, so a plain isAssignableFrom against the raw declaration is unreliable;
    // walk the declaration's supertypes by qualified name instead.
    override fun isScreenType(type: KSType, symbols: CircuitSymbols): Boolean {
      val declaration = type.declaration as? KSClassDeclaration ?: return false
      if (declaration.qualifiedName?.asString() == CircuitNames.SUB_SCREEN.canonicalName) {
        return true
      }
      return declaration.getAllSuperTypes().any {
        it.declaration.qualifiedName?.asString() == CircuitNames.SUB_SCREEN.canonicalName
      }
    }

    override fun isProvidedFunctionParameter(
      type: KSType,
      factoryType: FactoryType,
      symbols: CircuitSymbols,
    ): Boolean =
      symbols.subCircuitUiState.isAssignableFrom(type) || symbols.modifier.isAssignableFrom(type)

    override fun uiFunctionWrapperPrefix(): CodeBlock = CodeBlock.of("%T", CircuitNames.SUB_UI)

    override fun createParams(factoryType: FactoryType): List<ParameterSpec> =
      listOf(ParameterSpec.builder("screen", CircuitNames.SUB_SCREEN.parameterizedBy(STAR)).build())

    override fun createReturn(factoryType: FactoryType): TypeName =
      if (factoryType == FactoryType.UI) {
        CircuitNames.SUB_UI.parameterizedBy(STAR).copy(nullable = true)
      } else {
        CircuitNames.SUB_PRESENTER.parameterizedBy(STAR, STAR).copy(nullable = true)
      }

    // For the Dagger/Anvil assisted path the generated factory is named after the annotated
    // @AssistedFactory interface (e.g. TestPresenter.Factory -> TestPresenter_Factory). For the
    // provider/kotlin-inject path it's named after the target class.
    override fun classBaseName(
      annotatedDeclaration: KSClassDeclaration,
      targetClass: KSClassDeclaration,
      isAssisted: Boolean,
      isKotlinInjectAnvil: Boolean,
    ): String =
      if (isAssisted && !isKotlinInjectAnvil) {
        annotatedDeclaration.toClassName().simpleNames.joinToString("_")
      } else {
        targetClass.simpleName.getShortName()
      }

    override fun classPackageName(
      annotatedDeclaration: KSClassDeclaration,
      targetClass: KSClassDeclaration,
    ): String = annotatedDeclaration.packageName.asString()

    override fun factorySuffix(factoryType: FactoryType): String =
      if (factoryType == FactoryType.UI) {
        CircuitNames.SUB_UI_FACTORY_SUFFIX
      } else {
        CircuitNames.SUB_PRESENTER_FACTORY_SUFFIX
      }

    override fun notInjectableError(): String =
      "@$annotationSimpleName-annotated classes must be injectable: annotate the class or a " +
        "constructor with @Inject, or annotate an @AssistedFactory interface."
  }
}
