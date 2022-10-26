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
package com.slack.circuit.codegen

import com.google.auto.service.AutoService
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAllSuperTypes
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
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import java.util.ServiceLoader

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

private class CircuitSymbolProcessor(
  private val logger: KSPLogger,
  private val codeGenerator: CodeGenerator
) : SymbolProcessor {
  val extensions =
    ServiceLoader.load(
        CircuitProcessingExtension::class.java,
        CircuitProcessingExtension::class.java.classLoader
      )
      .iterator()
      .asSequence()
      .toSet()

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val circuitUiState =
      resolver
        .getClassDeclarationByName(
          resolver.getKSNameFromString("com.slack.circuit.CircuitUiState")
        )
        ?.asType(emptyList())
        ?: return emptyList()
    resolver.getSymbolsWithAnnotation(CIRCUIT_INJECT_ANNOTATION).forEach { annotatedElement ->
      when (annotatedElement) {
        is KSClassDeclaration -> {
          generateFactory(annotatedElement, InstantiationType.CLASS, circuitUiState)
        }
        is KSFunctionDeclaration -> {
          generateFactory(annotatedElement, InstantiationType.FUNCTION, circuitUiState)
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
    circuitUiState: KSType
  ) {
    val circuitInjectAnnotation =
      annotatedElement.annotations.first {
        it.annotationType.resolve().declaration.qualifiedName?.asString() ==
          CIRCUIT_INJECT_ANNOTATION
      }
    val screenType =
      circuitInjectAnnotation.annotationType.element!!.typeArguments[0].toTypeName()

    val className: String
    val packageName: String
    val factoryType: FactoryType
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
        codeBlock =
          when (factoryType) {
            FactoryType.PRESENTER ->
              CodeBlock.of(
                "%M·{·%M()·}",
                MemberName("com.slack.circuit", "presenterOf"),
                MemberName(packageName, name)
              )
            FactoryType.UI -> {
              val stateParam =
                fd.parameters.singleOrNull { parameter ->
                  parameter.type.resolve().isAssignableFrom(circuitUiState)
                }
              if (stateParam == null) {
                CodeBlock.of(
                  "%M<%T>·{·%M()·}",
                  MemberName("com.slack.circuit", "ui"),
                  CircuitUiState::class.java,
                  MemberName(packageName, name),
                )
              } else {
                CodeBlock.of(
                  "%M<%T>·{·state·->·%M(%L·=·state)·}",
                  MemberName("com.slack.circuit", "ui"),
                  stateParam.type.resolve().toTypeName(),
                  MemberName(packageName, name),
                  stateParam.name!!.getShortName()
                )
              }
            }
          }
      }
      InstantiationType.CLASS -> {
        val cd = annotatedElement as KSClassDeclaration
        className = cd.simpleName.getShortName()
        packageName = cd.packageName.getShortName()
        factoryType =
          cd
            .getAllSuperTypes()
            .mapNotNull {
              when (it.declaration.qualifiedName?.asString()) {
                CIRCUIT_UI -> FactoryType.UI
                CIRCUIT_PRESENTER -> FactoryType.PRESENTER
                else -> null
              }
            }
            .first()
        codeBlock = CodeBlock.of("%T()", className)
      }
    }

    val typeSpec =
      when (factoryType) {
        FactoryType.PRESENTER ->
          buildPresenterFactory(className, annotatedElement, screenType, codeBlock)
        FactoryType.UI ->
          buildUiFactory(className, annotatedElement, screenType, codeBlock)
      }

    val finalSpec =
      extensions.fold(typeSpec) { spec, extension ->
        extension.process(spec, annotatedElement, factoryType)
      }

    FileSpec.get(packageName, finalSpec).writeTo(codeGenerator = codeGenerator, aggregating = false)
  }
}

private fun buildUiFactory(
  className: String,
  originatingSymbol: KSAnnotated,
  screenType: TypeName,
  instantiationCodeBlock: CodeBlock
): TypeSpec {
  return TypeSpec.classBuilder(className + FACTORY)
    .addSuperinterface(Ui.Factory::class)
    .addFunction(
      FunSpec.builder("create")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("screen", Screen::class)
        .addParameter("circuitConfig", CircuitConfig::class)
        .returns(ScreenUi::class.asClassName().copy(nullable = true))
        .beginControlFlow("return when (screen)")
        .addStatement(
          "is %T -> %T(%L)",
          screenType,
          ScreenUi::class.asTypeName(),
          instantiationCodeBlock
        )
        .addStatement("else -> null")
        .endControlFlow()
        .build()
    )
    .addOriginatingKSFile(originatingSymbol.containingFile!!)
    .build()
}

private fun buildPresenterFactory(
  className: String,
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

  return TypeSpec.classBuilder(className + FACTORY)
    .addSuperinterface(Presenter.Factory::class)
    .addFunction(
      FunSpec.builder("create")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("screen", Screen::class)
        .addParameter("navigator", Navigator::class)
        .addParameter("circuitConfig", CircuitConfig::class)
        .returns(Presenter::class.asClassName().parameterizedBy(STAR).copy(nullable = true))
        .beginControlFlow("return when (screen)")
        .addStatement("is %T -> %L", screenType, instantiationCodeBlock)
        .addStatement("else -> null")
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
