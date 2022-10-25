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
package com.slack.circuit.codegen

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.slack.circuit.CircuitConfig
import com.slack.circuit.CircuitInject
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.ScreenUi
import com.slack.circuit.Ui
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import java.util.ServiceLoader

private val CIRCUIT_INJECT_ANNOTATION = CircuitInject::class.java.canonicalName
private val CIRCUIT_PRESENTER = Presenter::class.java.canonicalName
private val CIRCUIT_UI = Ui::class.java.canonicalName
private val FACTORY = "Factory"

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
  override fun process(resolver: Resolver): List<KSAnnotated> {
    val extensions = ServiceLoader.load(CircuitProcessingExtension::class.java, CircuitProcessingExtension::class.java.classLoader)
      .iterator()
      .asSequence()
      .toSet()
    resolver.getSymbolsWithAnnotation(CIRCUIT_INJECT_ANNOTATION).forEach { annotatedClass ->
      check(annotatedClass is KSClassDeclaration)
      val circuitInjectAnnotation =
        annotatedClass.annotations.first {
          it.annotationType.resolve().declaration.qualifiedName?.asString() ==
            CIRCUIT_INJECT_ANNOTATION
        }
      val screenType =
        circuitInjectAnnotation.annotationType.element!!.typeArguments.get(0).toTypeName()

      val factoryType =
        annotatedClass
          .getAllSuperTypes()
          .mapNotNull {
            when (it.declaration.qualifiedName?.asString()) {
              CIRCUIT_UI -> FactoryType.UI
              CIRCUIT_PRESENTER -> FactoryType.PRESENTER
              else -> null
            }
          }
          .first()

      val className = annotatedClass.simpleName.getShortName()
      val packageName = annotatedClass.packageName.getShortName()
      val typeSpec =
        when (factoryType) {
          FactoryType.PRESENTER -> buildPresenterFactory(className, annotatedClass, screenType)
          FactoryType.UI -> buildUiFactory(className, annotatedClass, screenType)
        }

      val finalSpec = extensions.fold(typeSpec) { spec, extension ->
        extension.process(spec, annotatedClass, factoryType)
      }

      FileSpec.get(packageName, finalSpec)
        .writeTo(codeGenerator = codeGenerator, aggregating = false)
    }
    return emptyList()
  }
}

private fun buildUiFactory(
  className: String,
  annotatedClass: KSClassDeclaration,
  screenType: TypeName
): TypeSpec {
  return TypeSpec.classBuilder(className + FACTORY)
    .addSuperinterface(Ui.Factory::class)
    .addFunction(
      FunSpec.builder("create")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("screen", Screen::class)
        .addParameter("navigator", Navigator::class)
        .addParameter("circuitConfig", CircuitConfig::class)
        .returns(ScreenUi::class.asClassName().copy(nullable = true))
        .beginControlFlow("return when (screen)")
        .addStatement(
          "is %T -> %T(%T())",
          screenType,
          ScreenUi::class.asTypeName(),
          annotatedClass.toClassName()
        )
        .addStatement("else -> null")
        .endControlFlow()
        .build()
    )
    .addOriginatingKSFile(annotatedClass.containingFile!!)
    .build()
}

private fun buildPresenterFactory(
  className: String,
  annotatedClass: KSClassDeclaration,
  screenType: TypeName
): TypeSpec {
  // The TypeSpec below will generate something similar to the following.
  //  public class AboutPresenterFactory : Presenter.Factory {
  //    public override fun create(
  //      screen: Screen,
  //      navigator: Navigator,
  //      circuitConfig: CircuitConfig,
  //    ): Presenter<*>? = when (screen) {
  //      is AboutScreen -> AboutPresenter()
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
        .addStatement("is %T -> %T()", screenType, annotatedClass.toClassName())
        .addStatement("else -> null")
        .endControlFlow()
        .build()
    )
    .addOriginatingKSFile(annotatedClass.containingFile!!)
    .build()
}

public enum class FactoryType {
  PRESENTER,
  UI
}
