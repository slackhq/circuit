// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnsafeCallOnNullableType")

package com.slack.circuit.codegen

import com.google.auto.service.AutoService
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Visibility
import com.slack.circuit.CircuitContext
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.ScreenUi
import com.slack.circuit.Ui
import com.slack.circuit.codegen.annotations.CircuitInject
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import dagger.assisted.AssistedFactory
import javax.inject.Inject
import javax.inject.Provider

private val CIRCUIT_INJECT_ANNOTATION = CircuitInject::class.java.canonicalName
private val CIRCUIT_PRESENTER = Presenter::class.java.canonicalName
private val CIRCUIT_UI = Ui::class.java.canonicalName
private const val FACTORY = "Factory"

@AutoService(SymbolProcessorProvider::class)
public class CircuitSymbolProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return CircuitSymbolProcessor(environment.logger, environment.codeGenerator)
  }
}

private class CircuitSymbols private constructor(resolver: Resolver) {
  val circuitUiState = resolver.loadKSType<CircuitUiState>()
  val screen = resolver.loadKSType<Screen>()
  val navigator = resolver.loadKSType<Navigator>()
  companion object {
    fun create(resolver: Resolver): CircuitSymbols? {
      @Suppress("SwallowedException")
      return try {
        CircuitSymbols(resolver)
      } catch (e: IllegalStateException) {
        null
      }
    }
  }
}

private inline fun <reified T> Resolver.loadKSType(): KSType {
  return loadOptionalKSType<T>()
    ?: error("Could not find ${T::class.java.canonicalName} in classpath")
}

private inline fun <reified T> Resolver.loadOptionalKSType(): KSType? {
  return getClassDeclarationByName(getKSNameFromString(T::class.java.canonicalName))
    ?.asType(emptyList())
}

private class CircuitSymbolProcessor(
  private val logger: KSPLogger,
  private val codeGenerator: CodeGenerator
) : SymbolProcessor {

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = CircuitSymbols.create(resolver) ?: return emptyList()
    resolver.getSymbolsWithAnnotation(CIRCUIT_INJECT_ANNOTATION).forEach { annotatedElement ->
      when (annotatedElement) {
        is KSClassDeclaration -> {
          generateFactory(annotatedElement, InstantiationType.CLASS, symbols)
        }
        is KSFunctionDeclaration -> {
          generateFactory(annotatedElement, InstantiationType.FUNCTION, symbols)
        }
        else ->
          logger.error(
            "CircuitInject is only applicable on classes and functions.",
            annotatedElement
          )
      }
    }
    return emptyList()
  }

  private fun generateFactory(
    annotatedElement: KSAnnotated,
    instantiationType: InstantiationType,
    symbols: CircuitSymbols
  ) {
    val circuitInjectAnnotation =
      annotatedElement.annotations.first {
        it.annotationType.resolve().declaration.qualifiedName?.asString() ==
          CIRCUIT_INJECT_ANNOTATION
      }
    val screenKSType = circuitInjectAnnotation.arguments[0].value as KSType
    val screenIsObject =
      screenKSType.declaration.let { it is KSClassDeclaration && it.classKind == ClassKind.OBJECT }
    val screenType = screenKSType.toTypeName()
    val scope = (circuitInjectAnnotation.arguments[1].value as KSType).toTypeName()

    val factoryData =
      computeFactoryData(annotatedElement, symbols, screenKSType, instantiationType, logger)
        ?: return

    val builder =
      TypeSpec.classBuilder(factoryData.className + FACTORY)
        .addAnnotation(
          AnnotationSpec.builder(ContributesMultibinding::class)
            .addMember("%T::class", scope)
            .build()
        )
        .primaryConstructor(
          FunSpec.constructorBuilder()
            .addAnnotation(Inject::class.java)
            .addParameters(factoryData.constructorParams)
            .build()
        )
        .apply {
          if (factoryData.constructorParams.isNotEmpty()) {
            for (param in factoryData.constructorParams) {
              addProperty(
                PropertySpec.builder(param.name, param.type, KModifier.PRIVATE)
                  .initializer(param.name)
                  .build()
              )
            }
          }
        }
    val screenBranch =
      if (screenIsObject) {
        CodeBlock.of("%T", screenType)
      } else {
        CodeBlock.of("is·%T", screenType)
      }
    val typeSpec =
      when (factoryData.factoryType) {
        FactoryType.PRESENTER ->
          builder.buildPresenterFactory(annotatedElement, screenBranch, factoryData.codeBlock)
        FactoryType.UI ->
          builder.buildUiFactory(annotatedElement, screenBranch, factoryData.codeBlock)
      }

    FileSpec.get(factoryData.packageName, typeSpec)
      .writeTo(codeGenerator = codeGenerator, aggregating = false)
  }

  private data class FactoryData(
    val className: String,
    val packageName: String,
    val factoryType: FactoryType,
    val constructorParams: List<ParameterSpec>,
    val codeBlock: CodeBlock
  )

  /** Computes the data needed to generate a factory. */
  // Detekt and ktfmt don't agree on whether or not the rectangle rule makes for readable code.
  @Suppress("ComplexMethod", "LongMethod")
  @OptIn(KspExperimental::class)
  private fun computeFactoryData(
    annotatedElement: KSAnnotated,
    symbols: CircuitSymbols,
    screenKSType: KSType,
    instantiationType: InstantiationType,
    logger: KSPLogger,
  ): FactoryData? {
    val className: String
    val packageName: String
    val factoryType: FactoryType
    val constructorParams = mutableListOf<ParameterSpec>()
    val codeBlock: CodeBlock

    when (instantiationType) {
      InstantiationType.FUNCTION -> {
        val fd = annotatedElement as KSFunctionDeclaration
        fd.checkVisibility(logger) {
          return null
        }
        val name = annotatedElement.simpleName.getShortName()
        className = name
        packageName = fd.packageName.asString()
        factoryType =
          if (name.endsWith("Presenter")) {
            FactoryType.PRESENTER
          } else {
            FactoryType.UI
          }
        val (_, assistedParams) =
          fd.assistedParameters(symbols, logger, screenKSType, factoryType == FactoryType.PRESENTER, false)
        codeBlock =
          when (factoryType) {
            FactoryType.PRESENTER ->
              CodeBlock.of(
                "%M·{·%M(%L)·}",
                MemberName("com.slack.circuit", "presenterOf"),
                MemberName(packageName, name),
                assistedParams
              )
            FactoryType.UI -> {
              val stateParam =
                fd.parameters.singleOrNull { parameter ->
                  symbols.circuitUiState.isAssignableFrom(parameter.type.resolve())
                }
              if (stateParam == null) {
                CodeBlock.of(
                  "%M<%T>·{·%M(%L)·}",
                  MemberName("com.slack.circuit", "ui"),
                  CircuitUiState::class.java,
                  MemberName(packageName, name),
                  assistedParams
                )
              } else {
                val block =
                  if (assistedParams.isEmpty()) {
                    CodeBlock.of("")
                  } else {
                    CodeBlock.of(",·%L", assistedParams)
                  }
                CodeBlock.of(
                  "%M<%T>·{·state·->·%M(%L·=·state%L)·}",
                  MemberName("com.slack.circuit", "ui"),
                  stateParam.type.resolve().toTypeName(),
                  MemberName(packageName, name),
                  stateParam.name!!.getShortName(),
                  block
                )
              }
            }
          }
      }
      InstantiationType.CLASS -> {
        val cd = annotatedElement as KSClassDeclaration
        cd.checkVisibility(logger) {
          return null
        }
        val isAssisted = cd.isAnnotationPresent(AssistedFactory::class)
        val creatorOrConstructor: KSFunctionDeclaration?
        val targetClass: KSClassDeclaration
        if (isAssisted) {
          val creatorFunction = cd.getAllFunctions().filter { it.isAbstract }.single()
          creatorOrConstructor = creatorFunction
          targetClass = creatorFunction.returnType!!.resolve().declaration as KSClassDeclaration
          targetClass.checkVisibility(logger) {
            return null
          }
        } else {
          creatorOrConstructor = cd.primaryConstructor
          targetClass = cd
        }
        val useProvider =
          !isAssisted && creatorOrConstructor?.isAnnotationPresent(Inject::class) == true
        className = targetClass.simpleName.getShortName()
        packageName = targetClass.packageName.asString()
        factoryType =
          targetClass
            .getAllSuperTypes()
            .mapNotNull {
              when (it.declaration.qualifiedName?.asString()) {
                CIRCUIT_UI -> FactoryType.UI
                CIRCUIT_PRESENTER -> FactoryType.PRESENTER
                else -> null
              }
            }
            .first()
        val empty = CodeBlock.of("") to CodeBlock.of("")
        val (args, assistedParams) =
          if (useProvider) {
            // Nothing to do here, we'll just use the provider directly.
            empty
          } else {
            creatorOrConstructor?.assistedParameters(
              symbols,
              logger,
              screenKSType,
              allowNavigator = factoryType == FactoryType.PRESENTER,
              allowNonCircuitDeps = true
            ) ?: empty
          }
        codeBlock =
          if (useProvider) {
            // Inject a Provider<TargetClass> that we'll call get() on.
            constructorParams.add(
              ParameterSpec.builder(
                  "provider",
                  Provider::class.asClassName().parameterizedBy(targetClass.toClassName())
                )
                .build()
            )
            CodeBlock.of("provider.get()")
          } else if (isAssisted) {
            // Inject the target class's assisted factory that we'll call its create() on.
            constructorParams.add(ParameterSpec.builder("factory", cd.toClassName()).build())
            buildAssistedFactoryCall(creatorOrConstructor!!.simpleName.getShortName(), args, assistedParams)
          } else {
            // Simple constructor call, no injection.
            CodeBlock.of("%T(%L)", targetClass.toClassName(), assistedParams)
          }
      }
    }
    return FactoryData(className, packageName, factoryType, constructorParams, codeBlock)
  }
}

private fun buildAssistedFactoryCall(methodName: String, args: CodeBlock, assistedParams: CodeBlock): CodeBlock {
  val call = CodeBlock.of(
    "factory.%L(%L)",
    methodName,
    assistedParams
  )
  if (args.isEmpty()) return call

  return CodeBlock.builder()
    .add(args)
    .add(call)
    .build()
}

private data class AssistedType(
  val factoryName: String,
  val type: TypeName,
  val name: String,
  val codeBlock: CodeBlock = CodeBlock.of("$name = $factoryName")
)

/**
 * Returns a [CodeBlock] representation of all named assisted parameters on this
 * [KSFunctionDeclaration] to be used in generated invocation code.
 *
 * Example: this function
 * ```kotlin
 * @Composable
 * fun HomePresenter(screen: Screen, navigator: Navigator)
 * ```
 *
 * Yields this CodeBlock: `screen = screen, navigator = navigator`
 */
private fun KSFunctionDeclaration.assistedParameters(
  symbols: CircuitSymbols,
  logger: KSPLogger,
  screenType: KSType,
  allowNavigator: Boolean,
  allowNonCircuitDeps: Boolean
): Pair<CodeBlock, CodeBlock> {
  val params = buildSet {
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
                param
              )
            }
          }
          allowNonCircuitDeps -> {
            val name = param.name!!.getShortName()
            val factoryName = "context"
            addOrError(
              AssistedType(
                factoryName,
                type.toTypeName(),
                name,
                buildNonCircuitDep(name, factoryName, type)
              )
            )
          }
        }
      }
    }
    .toList()
    .map { it.codeBlock }
    .joinToCode(",·")

  return CodeBlock.of("") to params
}

private fun buildNonCircuitDep(name: String, factoryName: String, type: KSType): CodeBlock {
  val className = type.declaration.simpleName.asString()
  val retrieval = "$factoryName.tag($className::class)"
    .let { retrieval ->
      when {
        type.isMarkedNullable -> retrieval
        else -> """checkNotNull($retrieval) { "CircuitContext.tag(Class) returned a null value for required type '$className'" }"""
      }
    }
  return CodeBlock.of("$name = $retrieval")
}

private fun KSType.isSameDeclarationAs(type: KSType): Boolean {
  return this.declaration == type.declaration
}

private fun KSType.isInstanceOf(type: KSType): Boolean {
  return type.isAssignableFrom(this)
}

private fun TypeSpec.Builder.buildUiFactory(
  originatingSymbol: KSAnnotated,
  screenBranch: CodeBlock,
  instantiationCodeBlock: CodeBlock
): TypeSpec {
  return addSuperinterface(Ui.Factory::class)
    .addFunction(
      FunSpec.builder("create")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("screen", Screen::class)
        .addParameter("context", CircuitContext::class)
        .returns(ScreenUi::class.asClassName().copy(nullable = true))
        .beginControlFlow("return·when·(screen)")
        .addStatement(
          "%L·->·%T(%L)",
          screenBranch,
          ScreenUi::class.asTypeName(),
          instantiationCodeBlock
        )
        .addStatement("else·->·null")
        .endControlFlow()
        .build()
    )
    .addOriginatingKSFile(originatingSymbol.containingFile!!)
    .build()
}

private fun TypeSpec.Builder.buildPresenterFactory(
  originatingSymbol: KSAnnotated,
  screenBranch: CodeBlock,
  instantiationCodeBlock: CodeBlock
): TypeSpec {
  // The TypeSpec below will generate something similar to the following.
  //  public class AboutPresenterFactory : Presenter.Factory {
  //    public override fun create(
  //      screen: Screen,
  //      navigator: Navigator,
  //      context: CircuitContext,
  //    ): Presenter<*>? = when (screen) {
  //      is AboutScreen -> AboutPresenter()
  //      is AboutScreen -> presenterOf { AboutPresenter() }
  //      else -> null
  //    }
  //  }

  return addSuperinterface(Presenter.Factory::class)
    .addFunction(
      FunSpec.builder("create")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("screen", Screen::class)
        .addParameter("navigator", Navigator::class)
        .addParameter("context", CircuitContext::class)
        .returns(Presenter::class.asClassName().parameterizedBy(STAR).copy(nullable = true))
        .beginControlFlow("return when (screen)")
        .addStatement("%L·->·%L", screenBranch, instantiationCodeBlock)
        .addStatement("else·->·null")
        .endControlFlow()
        .build()
    )
    .addOriginatingKSFile(originatingSymbol.containingFile!!)
    .build()
}

private enum class FactoryType {
  PRESENTER,
  UI
}

private enum class InstantiationType {
  FUNCTION,
  CLASS
}

private inline fun KSDeclaration.checkVisibility(logger: KSPLogger, returnBody: () -> Unit) {
  if (!getVisibility().isVisible) {
    logger.error("CircuitInject is not applicable to private or local functions and classes.", this)
    returnBody()
  }
}

private val Visibility.isVisible: Boolean
  get() = this != Visibility.PRIVATE && this != Visibility.LOCAL
