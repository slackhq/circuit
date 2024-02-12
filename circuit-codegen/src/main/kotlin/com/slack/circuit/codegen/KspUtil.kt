package com.slack.circuit.codegen

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Visibility
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.toTypeName

internal fun Resolver.loadKSType(name: String): KSType =
  loadOptionalKSType(name) ?: error("Could not find $name in classpath")

internal fun Resolver.loadOptionalKSType(name: String?): KSType? {
  if (name == null) return null
  return getClassDeclarationByName(getKSNameFromString(name))?.asType(emptyList())
}

internal inline fun KSDeclaration.checkVisibility(logger: KSPLogger, returnBody: () -> Unit) {
  if (!getVisibility().isVisible) {
    logger.error("CircuitInject is not applicable to private or local functions and classes.", this)
    returnBody()
  }
}

internal fun KSDeclaration.topLevelDeclaration(): KSDeclaration {
  return parentDeclaration?.topLevelDeclaration() ?: this
}

internal val Visibility.isVisible: Boolean
  get() = this != Visibility.PRIVATE && this != Visibility.LOCAL

internal data class AssistedType(val factoryName: String, val type: TypeName, val name: String)

/**
 * Returns a [CodeBlock] representation of all named assisted parameters on this
 * [KSFunctionDeclaration] to be used in generated invocation code.
 *
 * Example: this function
 *
 * ```kotlin
 * @Composable
 * fun HomePresenter(screen: Screen, navigator: Navigator)
 * ```
 *
 * Yields this CodeBlock: `screen = screen, navigator = navigator`
 */
internal fun KSFunctionDeclaration.assistedParameters(
  symbols: CircuitSymbols,
  logger: KSPLogger,
  screenType: KSType,
  allowNavigator: Boolean,
): CodeBlock {
  return buildSet {
    for (param in parameters) {
      fun <E> MutableSet<E>.addOrError(element: E) {
        val added = add(element)
        if (!added) {
          logger.error("Multiple parameters of type $element are not allowed.", param)
        }
      }

      val type = param.type.resolve()
      when {
        type.isInstanceOf(symbols.screen) -> {
          if (screenType.isSameDeclarationAs(type)) {
            addOrError(AssistedType("screen", type.toTypeName(), param.name!!.getShortName()))
          } else {
            logger.error("Screen type mismatch. Expected $screenType but found $type", param)
          }
        }
        type.isInstanceOf(symbols.navigator) -> {
          if (allowNavigator) {
            addOrError(AssistedType("navigator", type.toTypeName(), param.name!!.getShortName()))
          } else {
            logger.error(
              "Navigator type mismatch. Navigators are not injectable on this type.",
              param,
            )
          }
        }
      }
    }
  }
    .toList()
    .map { CodeBlock.of("${it.name} = ${it.factoryName}") }
    .joinToCode(",·")
}

internal fun KSType.isSameDeclarationAs(type: KSType): Boolean {
  return this.declaration == type.declaration
}

internal fun KSType.isInstanceOf(type: KSType): Boolean {
  return type.isAssignableFrom(this)
}