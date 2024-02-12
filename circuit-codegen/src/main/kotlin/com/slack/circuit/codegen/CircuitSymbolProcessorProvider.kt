// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnsafeCallOnNullableType")

package com.slack.circuit.codegen

import com.google.auto.service.AutoService
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.PlatformInfo
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import dagger.assisted.AssistedFactory
import java.util.Locale
import javax.inject.Inject

@AutoService(SymbolProcessorProvider::class)
public class CircuitSymbolProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return CircuitSymbolProcessor(
      environment.logger,
      environment.codeGenerator,
      environment.options,
      environment.platforms,
    )
  }
}

private class CircuitSymbolProcessor(
  private val logger: KSPLogger,
  private val codeGenerator: CodeGenerator,
  private val options: Map<String, String>,
  private val platforms: List<PlatformInfo>,
) : SymbolProcessor {

  private val lenient = options["circuit.codegen.lenient"]?.toBoolean() ?: false

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = CircuitSymbols.create(resolver) ?: return emptyList()
    val codegenMode =
      options[CircuitNames.CIRCUIT_CODEGEN_MODE].let { mode ->
        if (mode == null) {
          CodegenMode.ANVIL
        } else {
          CodegenMode.entries.find { it.name.equals(mode, ignoreCase = true) }
            ?: run {
              logger.error("Unrecognised option for codegen mode \"$mode\".")
              return emptyList()
            }
        }
      }

    if (!codegenMode.supportsPlatforms(platforms)) {
      logger.error("Unsupported platforms for codegen mode ${codegenMode.name}. $platforms")
      return emptyList()
    }

    resolver
      .getSymbolsWithAnnotation(CircuitNames.CIRCUIT_INJECT_ANNOTATION.canonicalName)
      .forEach { annotatedElement ->
        when (annotatedElement) {
          is KSClassDeclaration ->
            generateFactory(annotatedElement, InstantiationType.CLASS, symbols, codegenMode)
          is KSFunctionDeclaration ->
            generateFactory(annotatedElement, InstantiationType.FUNCTION, symbols, codegenMode)
          else ->
            logger.error(
              "CircuitInject is only applicable on classes and functions.",
              annotatedElement,
            )
        }
      }
    return emptyList()
  }

  private fun generateFactory(
    annotatedElement: KSAnnotated,
    instantiationType: InstantiationType,
    symbols: CircuitSymbols,
    codegenMode: CodegenMode,
  ) {
    val circuitInjectAnnotation =
      annotatedElement.annotations.first {
        it.annotationType.resolve().declaration.qualifiedName?.asString() ==
          CircuitNames.CIRCUIT_INJECT_ANNOTATION.canonicalName
      }
    val screenKSType = circuitInjectAnnotation.arguments[0].value as KSType
    // TODO why does smart cast not work on objects?
    val screenIsObject = false
    //      screenKSType.declaration.let { it is KSClassDeclaration && it.classKind ==
    // ClassKind.OBJECT }
    val screenType = screenKSType.toTypeName()
    val scope = (circuitInjectAnnotation.arguments[1].value as KSType).toTypeName()

    val factoryData =
      computeFactoryData(
        annotatedElement,
        symbols,
        screenKSType,
        instantiationType,
        logger,
        codegenMode,
      ) ?: return

    val className =
      factoryData.className.replaceFirstChar { char ->
        char.takeIf { char.isLowerCase() }?.run { uppercase(Locale.getDefault()) }
          ?: char.toString()
      }

    val builder =
      TypeSpec.classBuilder(className + CircuitNames.FACTORY)
        .primaryConstructor(
          FunSpec.constructorBuilder()
            .addAnnotation(codegenMode.injectAnnotations.inject)
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

          codegenMode.annotateFactory(builder = this, scope = scope)
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

    // Note: We can't directly reference the top-level class declaration for top-level functions in
    // kotlin. For annotatedElements which as top-level functions, topLevelClass will be null.
    val topLevelDeclaration = (annotatedElement as KSDeclaration).topLevelDeclaration()
    val topLevelClass = (topLevelDeclaration as? KSClassDeclaration)?.toClassName()

    val originatingFile = listOfNotNull(annotatedElement.containingFile)

    FileSpec.get(factoryData.packageName, typeSpec)
      .writeTo(
        codeGenerator = codeGenerator,
        aggregating = false,
        originatingKSFiles = originatingFile,
      )

    val additionalType =
      codegenMode.produceAdditionalTypeSpec(
        factoryType = factoryData.factoryType,
        factory = ClassName(factoryData.packageName, className + CircuitNames.FACTORY),
        scope = scope,
        topLevelClass = topLevelClass,
      ) ?: return

    FileSpec.get(factoryData.packageName, additionalType)
      .writeTo(
        codeGenerator = codeGenerator,
        aggregating = false,
        originatingKSFiles = originatingFile,
      )
  }

  private data class FactoryData(
    val className: String,
    val packageName: String,
    val factoryType: FactoryType,
    val constructorParams: List<ParameterSpec>,
    val codeBlock: CodeBlock,
  )

  /** Computes the data needed to generate a factory. */
  // Detekt and ktfmt don't agree on whether or not the rectangle rule makes for readable code.
  @Suppress("ComplexMethod", "LongMethod", "ReturnCount")
  @OptIn(KspExperimental::class)
  private fun computeFactoryData(
    annotatedElement: KSAnnotated,
    symbols: CircuitSymbols,
    screenKSType: KSType,
    instantiationType: InstantiationType,
    logger: KSPLogger,
    codegenMode: CodegenMode,
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
        val assistedParams =
          fd.assistedParameters(
            symbols = symbols,
            logger = logger,
            screenType = screenKSType,
            allowNavigator = factoryType == FactoryType.PRESENTER,
            codegenMode = codegenMode,
          )
        codeBlock =
          when (factoryType) {
            FactoryType.PRESENTER ->
              CodeBlock.of(
                "%M·{·%M(%L)·}",
                MemberName(CircuitNames.CIRCUIT_RUNTIME_PRESENTER_PACKAGE, "presenterOf"),
                MemberName(packageName, name),
                assistedParams,
              )
            FactoryType.UI -> {
              // State param is optional
              val stateParam =
                fd.parameters.singleOrNull { parameter ->
                  symbols.circuitUiState.isAssignableFrom(parameter.type.resolve())
                }

              // Modifier param is required
              val modifierParam =
                fd.parameters.singleOrNull { parameter ->
                  symbols.modifier.isAssignableFrom(parameter.type.resolve())
                }
                  ?: run {
                    logger.error("UI composable functions must have a Modifier parameter!", fd)
                    return null
                  }

              /*
              Diagram of what goes into generating a function!
              - State parameter is _optional_ and can be omitted if it's static state.
                - When omitted, the argument becomes _ and the param is omitted entirely.
              - <StateType> is either the State or CircuitUiState if no state param is used.
              - Modifier parameter is required.
              - Assisted parameters can be 0 or more extra supported assisted parameters.

                                                           Optional state param
                            Optional state arg                   │
                                │                                │       Required modifier param
                                │      Req modifier arg          │               │
              ┌─── ui function  │       │                        │               │            Any assisted params
              │                 │       │       Composable       │               │                    │
              │   State type    │       │          │             │               │                    │
              │      │          │       │          │             │               │                    │
              │      │          │       │          │             │               │                    │
              └──────┴─────   ──┴──  ───┴────    ──┴───── ───────┴─────  ────────┴──────────  ────────┴────────
              ui<StateType> { state, modifier -> Function(state = state, modifier = modifier, <assisted params>) }
              ────────────────────────────────────────────────────────────────────────────────────────────────────

              Diagram generated with asciiflow. You can make new ones or edit with this link.
              https://asciiflow.com/#/share/eJzVVM1KxDAQfpVhTgr1IizLlt2CCF4F9ZhLdGclkKbd%2FMCW0rfwcXwan8SsWW27sd0qXixzmDTffN83bSY1Kp4TpspJmaDkFWlMsWa4Y5guZvOEYeWzy%2FnCZ5Z21i8Ywg%2Be29KKQnEJxnJLUHLNc8bUKIjr55jo7eXVR1R62Dplo4Xc0dYJTWvIi7XYCNIDnnpVvqjF9%2BzF2sFlsBvCCdg49bTvsVcx4vtb2u7ySlXAjRHG%2BlY%2BOjBBdYSqza6LvCwMf5Q0VS%2FqDjq%2FzVYlDfV1zPMrpWHSf6w1JeDz4HfejGCH9ybrTQT%2BUan%2FEk4s7%2Fdj%2F%2BAPUQZ1uAOSdtwuMrg5TM9ZuB9WEWb1lSawPBqL7Bwahg027%2Byjz8s%3D)
              */

              @Suppress("IfThenToElvis") // The elvis is less readable here
              val stateType =
                if (stateParam == null) CircuitNames.CIRCUIT_UI_STATE
                else stateParam.type.resolve().toTypeName()
              val stateArg = if (stateParam == null) "_" else "state"
              val stateParamBlock =
                if (stateParam == null) CodeBlock.of("")
                else CodeBlock.of("%L·=·state,·", stateParam.name!!.getShortName())
              val modifierParamBlock =
                CodeBlock.of("%L·=·modifier", modifierParam.name!!.getShortName())
              val assistedParamsBlock =
                if (assistedParams.isEmpty()) {
                  CodeBlock.of("")
                } else {
                  CodeBlock.of(",·%L", assistedParams)
                }
              CodeBlock.of(
                "%M<%T>·{·%L,·modifier·->·%M(%L%L%L)·}",
                MemberName(CircuitNames.CIRCUIT_RUNTIME_UI_PACKAGE, "ui"),
                stateType,
                stateArg,
                MemberName(packageName, name),
                stateParamBlock,
                modifierParamBlock,
                assistedParamsBlock,
              )
            }
          }
      }
      InstantiationType.CLASS -> {
        val declaration = annotatedElement as KSClassDeclaration
        declaration.checkVisibility(logger) {
          return null
        }
        val isAssisted =
          if (lenient) {
            declaration.annotations.any { it.shortName.asString().contains("AssistedFactory") }
          } else {
            declaration.isAnnotationPresent(AssistedFactory::class)
          }
        val creatorOrConstructor: KSFunctionDeclaration?
        val targetClass: KSClassDeclaration
        if (isAssisted) {
          val creatorFunction = declaration.getAllFunctions().filter { it.isAbstract }.single()
          creatorOrConstructor = creatorFunction
          targetClass = creatorFunction.returnType!!.resolve().declaration as KSClassDeclaration
          targetClass.checkVisibility(logger) {
            return null
          }
        } else {
          creatorOrConstructor = declaration.primaryConstructor
          targetClass = declaration
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
                CircuitNames.CIRCUIT_UI.canonicalName -> FactoryType.UI
                CircuitNames.CIRCUIT_PRESENTER.canonicalName -> FactoryType.PRESENTER
                else -> null
              }
            }
            .firstOrNull()
            ?: run {
              val annotationsString =
                declaration.annotations.toList().joinToString {
                  it.annotationType.resolve().toTypeName().toString()
                }
              logger.error(
                "Factory must be for a UI or Presenter class, but was " +
                  "${targetClass.qualifiedName?.asString()}.\n" +
                  "Supertypes: ${targetClass.getAllSuperTypes().toList()}.\n" +
                  "isAssisted? ${isAssisted}\n" +
                  "Annotations: $annotationsString}",
                targetClass,
              )
              return null
            }
        val assistedParams =
          if (useProvider) {
            // Nothing to do here, we'll just use the provider directly.
            CodeBlock.of("")
          } else {
            creatorOrConstructor?.assistedParameters(
              symbols = symbols,
              logger = logger,
              screenType = screenKSType,
              allowNavigator = factoryType == FactoryType.PRESENTER,
              codegenMode = codegenMode,
            )
          }
        codeBlock =
          if (useProvider) {
            // Inject a Provider<TargetClass> that we'll call get() on.
            constructorParams.add(
              ParameterSpec.builder(
                  "provider",
                  ClassName("javax.inject", "Provider").parameterizedBy(targetClass.toClassName()),
                )
                .build()
            )
            CodeBlock.of("provider.get()")
          } else if (isAssisted) {
            // Inject the target class's assisted factory that we'll call its create() on.
            constructorParams.add(
              ParameterSpec.builder("factory", declaration.toClassName()).build()
            )
            CodeBlock.of(
              "factory.%L(%L)",
              creatorOrConstructor!!.simpleName.getShortName(),
              assistedParams,
            )
          } else {
            if (codegenMode == CodegenMode.KOTLIN_INJECT) {
              declaration.primaryConstructor?.parameters?.forEach { parameter ->
                val resolvedType = parameter.type.resolve()
                if (
                  !resolvedType.isInstanceOf(symbols.screen) &&
                    !resolvedType.isInstanceOf(symbols.navigator)
                ) {
                  constructorParams.add(
                    ParameterSpec.builder(parameter.name!!.asString(), resolvedType.toTypeName())
                      .build()
                  )
                }
              }
            }
            // Simple constructor call, no injection.
            CodeBlock.of("%T(%L)", targetClass.toClassName(), assistedParams)
          }
      }
    }
    return FactoryData(className, packageName, factoryType, constructorParams, codeBlock)
  }
}

private fun TypeSpec.Builder.buildUiFactory(
  originatingSymbol: KSAnnotated,
  screenBranch: CodeBlock,
  instantiationCodeBlock: CodeBlock,
): TypeSpec {
  return addSuperinterface(CircuitNames.CIRCUIT_UI_FACTORY)
    .addFunction(
      FunSpec.builder("create")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("screen", CircuitNames.SCREEN)
        .addParameter("context", CircuitNames.CIRCUIT_CONTEXT)
        .returns(CircuitNames.CIRCUIT_UI.parameterizedBy(STAR).copy(nullable = true))
        .beginControlFlow("return·when·(screen)")
        .addStatement("%L·->·%L", screenBranch, instantiationCodeBlock)
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
  instantiationCodeBlock: CodeBlock,
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

  return addSuperinterface(CircuitNames.CIRCUIT_PRESENTER_FACTORY)
    .addFunction(
      FunSpec.builder("create")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("screen", CircuitNames.SCREEN)
        .addParameter("navigator", CircuitNames.NAVIGATOR)
        .addParameter("context", CircuitNames.CIRCUIT_CONTEXT)
        .returns(CircuitNames.CIRCUIT_PRESENTER.parameterizedBy(STAR).copy(nullable = true))
        .beginControlFlow("return when (screen)")
        .addStatement("%L·->·%L", screenBranch, instantiationCodeBlock)
        .addStatement("else·->·null")
        .endControlFlow()
        .build()
    )
    .addOriginatingKSFile(originatingSymbol.containingFile!!)
    .build()
}
