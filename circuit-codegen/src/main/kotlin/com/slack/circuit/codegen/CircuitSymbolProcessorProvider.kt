/*
 * Copyright (C) 2022 Slack Technologies, LLC
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
@file:Suppress("UnsafeCallOnNullableType")
@file:OptIn(KspExperimental::class)

package com.slack.circuit.codegen

import com.google.auto.service.AutoService
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.slack.circuit.CircuitConfig
import com.slack.circuit.CircuitInject
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.ScreenUi
import com.slack.circuit.Ui
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
  val circuitConfig = resolver.loadKSType<CircuitConfig>()
  companion object {
    fun create(resolver: Resolver): CircuitSymbols? {
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

  @OptIn(KspExperimental::class)
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
    val screenType = screenKSType.toTypeName()
    val scope = (circuitInjectAnnotation.arguments[1].value as KSType).toTypeName()

    val className: String
    val packageName: String
    val factoryType: FactoryType
    val constructorParams = mutableListOf<ParameterSpec>()
    val codeBlock: CodeBlock

    when (instantiationType) {
      InstantiationType.FUNCTION -> {
        val fd = annotatedElement as KSFunctionDeclaration
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
          fd.assistedParameters(symbols, logger, screenKSType, factoryType == FactoryType.PRESENTER)
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
        val isAssisted = cd.isAnnotationPresent(AssistedFactory::class)
        val creatorOrConstructor: KSFunctionDeclaration?
        val targetClass =
          if (isAssisted) {
            val creatorFunction = cd.getAllFunctions().filter { it.isAbstract }.single()
            creatorOrConstructor = creatorFunction
            creatorFunction.returnType!!.resolve().declaration as KSClassDeclaration
          } else {
            creatorOrConstructor = cd.primaryConstructor
            cd
          }
        className = targetClass.simpleName.getShortName()
        packageName = targetClass.packageName.getShortName()
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
        val assistedParams =
          creatorOrConstructor?.assistedParameters(
            symbols,
            logger,
            screenKSType,
            allowNavigator = factoryType == FactoryType.PRESENTER
          )
        codeBlock =
          if (isAssisted) {
            constructorParams.add(ParameterSpec.builder("factory", cd.toClassName()).build())
            CodeBlock.of(
              "factory.%L(%L)",
              creatorOrConstructor!!.simpleName.getShortName(),
              assistedParams
            )
          } else {
            CodeBlock.of("%T(%L)", className, assistedParams)
          }
      }
    }

    val builder =
      TypeSpec.classBuilder(className + FACTORY)
        .addAnnotation(
          AnnotationSpec.builder(ContributesMultibinding::class)
            .addMember("%T::class", scope)
            .build()
        )
        .primaryConstructor(
          FunSpec.constructorBuilder()
            .addAnnotation(Inject::class.java)
            .addParameters(constructorParams)
            .build()
        )
        .apply {
          if (constructorParams.isNotEmpty()) {
            for (param in constructorParams) {
              addProperty(
                PropertySpec.builder(param.name, param.type, KModifier.PRIVATE)
                  .initializer(param.name)
                  .build()
              )
            }
          }
        }
    val typeSpec =
      when (factoryType) {
        FactoryType.PRESENTER ->
          builder.buildPresenterFactory(annotatedElement, screenType, codeBlock)
        FactoryType.UI -> builder.buildUiFactory(annotatedElement, screenType, codeBlock)
      }

    FileSpec.get(packageName, typeSpec).writeTo(codeGenerator = codeGenerator, aggregating = false)
  }
}

private data class AssistedType(
  val factoryName: String,
  val type: TypeName,
  val name: String,
)

private fun KSFunctionDeclaration.assistedParameters(
  symbols: CircuitSymbols,
  logger: KSPLogger,
  screenType: KSType,
  allowNavigator: Boolean
): CodeBlock {
  return buildSet {
      for (param in parameters) {
        val type = param.type.resolve()
        if (symbols.screen.isAssignableFrom(type)) {
          if (screenType.isAssignableFrom(type)) {
            val added = add(AssistedType("screen", type.toTypeName(), param.name!!.getShortName()))
            if (!added) {
              logger.error("Multiple parameters of type $type are not allowed.", param)
            }
          } else {
            logger.error("Screen type mismatch. Expected $screenType but found $type", param)
          }
        } else if (symbols.navigator.isAssignableFrom(type)) {
          if (allowNavigator) {
            val added =
              add(AssistedType("navigator", type.toTypeName(), param.name!!.getShortName()))
            if (!added) {
              logger.error("Multiple parameters of type $type are not allowed.", param)
            }
          } else {
            logger.error(
              "Navigator type mismatch. Navigators are not injectable on this type.",
              param
            )
          }
        } else if (symbols.circuitConfig.isAssignableFrom(type)) {
          val added =
            add(AssistedType("circuitConfig", type.toTypeName(), param.name!!.getShortName()))
          if (!added) {
            logger.error("Multiple parameters of type $type are not allowed.", param)
          }
        }
      }
    }
    .toList()
    .map { CodeBlock.of("${it.name} = ${it.factoryName}") }
    .joinToCode(",·")
}

private fun TypeSpec.Builder.buildUiFactory(
  originatingSymbol: KSAnnotated,
  screenType: TypeName,
  instantiationCodeBlock: CodeBlock
): TypeSpec {
  return addSuperinterface(Ui.Factory::class)
    .addFunction(
      FunSpec.builder("create")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("screen", Screen::class)
        .addParameter("circuitConfig", CircuitConfig::class)
        .returns(ScreenUi::class.asClassName().copy(nullable = true))
        .beginControlFlow("return·when·(screen)")
        .addStatement(
          "is·%T·->·%T(%L)",
          screenType,
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
  screenType: TypeName,
  instantiationCodeBlock: CodeBlock
): TypeSpec {
  // The TypeSpec below will generate something similar to the following.
  //  public class AboutPresenterFactory : Presenter.Factory {
  //    public override fun create(
  //      screen: Screen,
  //      navigator: Navigator,
  //      circuitConfig: CircuitConfig,
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
        .addParameter("circuitConfig", CircuitConfig::class)
        .returns(Presenter::class.asClassName().parameterizedBy(STAR).copy(nullable = true))
        .beginControlFlow("return when (screen)")
        .addStatement("is·%T·->·%L", screenType, instantiationCodeBlock)
        .addStatement("else·->·null")
        .endControlFlow()
        .build()
    )
    .addOriginatingKSFile(originatingSymbol.containingFile!!)
    .build()
}

public enum class FactoryType {
  PRESENTER,
  UI
}

public enum class InstantiationType {
  FUNCTION,
  CLASS
}
