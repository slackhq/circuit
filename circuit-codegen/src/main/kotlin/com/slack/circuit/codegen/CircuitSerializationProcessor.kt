// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen

import com.google.auto.service.AutoService
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.PlatformInfo
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Visibility
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * Generates one multibinding contribution for each opted-in `Screen` or `PopResult` type.
 *
 * Metro mode generates code like this:
 * ```kotlin
 * @Inject
 * @ContributesIntoSet(AppScope::class)
 * class HomeScreenCircuitSerializerRegistration : CircuitSerializerRegistration {
 *   override fun register(builder: PolymorphicModuleBuilder<CircuitSaveable>) {
 *     builder.subclass(subclass = HomeScreen::class, serializer = HomeScreen.serializer())
 *   }
 * }
 * ```
 *
 * Only the DI annotations differ between modes.
 */
internal class CircuitSerializationProcessor(
  private val logger: KSPLogger,
  private val codeGenerator: CodeGenerator,
  options: Map<String, String>,
  private val platforms: List<PlatformInfo>,
) : SymbolProcessor {
  private val options = CircuitOptions.load(options, logger)
  private val mode = this.options.mode

  @AutoService(SymbolProcessorProvider::class)
  public class Provider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
      return CircuitSerializationProcessor(
        environment.logger,
        environment.codeGenerator,
        environment.options,
        environment.platforms,
      )
    }
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    if (options === CircuitOptions.UNKNOWN) return emptyList()

    if (!mode.supportsPlatforms(platforms)) {
      logger.error("Unsupported platforms for codegen mode ${mode.name}. $platforms")
      return emptyList()
    }

    val deferred = mutableListOf<KSAnnotated>()
    resolver
      .getSymbolsWithAnnotation(CircuitNames.CIRCUIT_SERIALIZABLE.canonicalName, inDepth = true)
      .forEach { symbol ->
        if (!symbol.validate()) {
          deferred += symbol
        } else if (symbol is KSClassDeclaration) {
          // Platform KSP returns both the expect and actual declarations. The expect declaration
          // owns the generated registration, so skip actual declarations.
          if (Modifier.ACTUAL !in symbol.modifiers) {
            generateRegistration(symbol)
          }
        } else {
          logger.error("@CircuitSerializable is only applicable to classes and objects.", symbol)
        }
      }
    return deferred
  }

  private fun generateRegistration(declaration: KSClassDeclaration) {
    if (!declaration.isAccessibleFromGeneratedTopLevel()) {
      logger.error(
        "@CircuitSerializable is not applicable to private, protected, or local declarations.",
        declaration,
      )
      return
    }
    if (declaration.classKind == ClassKind.INTERFACE) {
      logger.error("@CircuitSerializable is not applicable to interfaces.", declaration)
      return
    }
    if (declaration.classKind != ClassKind.CLASS && declaration.classKind != ClassKind.OBJECT) {
      logger.error("@CircuitSerializable is only applicable to classes and objects.", declaration)
      return
    }
    if (Modifier.ABSTRACT in declaration.modifiers && Modifier.EXPECT !in declaration.modifiers) {
      logger.error("@CircuitSerializable is not applicable to abstract classes.", declaration)
      return
    }
    if (Modifier.INNER in declaration.modifiers) {
      logger.error("@CircuitSerializable is not applicable to inner classes.", declaration)
      return
    }
    if (declaration.typeParameters.isNotEmpty()) {
      logger.error("@CircuitSerializable is not applicable to generic classes.", declaration)
      return
    }

    val supportedSupertypeNames =
      setOf(CircuitNames.SCREEN.canonicalName, CircuitNames.POP_RESULT.canonicalName)

    if (
      declaration.getAllSuperTypes().none {
        it.declaration.qualifiedName?.asString() in supportedSupertypeNames
      }
    ) {
      logger.error(
        "@CircuitSerializable is only applicable to Screen and PopResult implementations.",
        declaration,
      )
      return
    }

    val annotation =
      declaration.annotations.singleOrNull {
        it.annotationType.resolve().declaration.qualifiedName?.asString() ==
          CircuitNames.CIRCUIT_SERIALIZABLE.canonicalName
      } ?: return

    val scope =
      (annotation.arguments.singleOrNull { it.name?.asString() == "scope" }?.value as? KSType)
        ?.toTypeName()
        ?: run {
          logger.error("Could not resolve @CircuitSerializable's scope.", declaration)
          return
        }

    val originatingFile =
      declaration.containingFile
        ?: run {
          logger.error("Could not find the source file containing this declaration.", declaration)
          return
        }

    val serializedType = declaration.toClassName()
    val generatedName =
      serializedType.simpleNames.joinToString("_") + "CircuitSerializerRegistration"
    val generatedType = ClassName(declaration.packageName.asString(), generatedName)
    val topLevelClass = declaration.topLevelClass().toClassName()

    val constructor = FunSpec.constructorBuilder()
    val registrationBuilder = TypeSpec.classBuilder(generatedType)
    mode.addInjectAnnotation(registrationBuilder, constructor, options)
    if (constructor.annotations.isNotEmpty()) {
      registrationBuilder.primaryConstructor(constructor.build())
    }
    mode.annotateFactory(registrationBuilder, scope)
    val registerFunction =
      FunSpec.builder("register")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter(
          "builder",
          CircuitNames.POLYMORPHIC_MODULE_BUILDER.parameterizedBy(CircuitNames.CIRCUIT_SAVEABLE),
        )
        .addStatement(
          "builder.subclass(subclass = %T::class, serializer = %T.serializer())",
          serializedType,
          serializedType,
        )
        .build()

    val registration =
      registrationBuilder
        .addSuperinterface(CircuitNames.CIRCUIT_SERIALIZER_REGISTRATION)
        .addFunction(registerFunction)
        .addOriginatingKSFile(originatingFile)
        .build()

    FileSpec.get(generatedType.packageName, registration)
      .writeTo(
        codeGenerator = codeGenerator,
        aggregating = false,
        originatingKSFiles = listOf(originatingFile),
      )

    val additionalType =
      mode.produceAdditionalMultibindingTypeSpec(
        implementation = generatedType,
        boundType = CircuitNames.CIRCUIT_SERIALIZER_REGISTRATION,
        scope = scope,
        topLevelClass = topLevelClass,
      ) ?: return

    FileSpec.get(generatedType.packageName, additionalType)
      .writeTo(
        codeGenerator = codeGenerator,
        aggregating = false,
        originatingKSFiles = listOf(originatingFile),
      )
  }
}

private fun KSDeclaration.isAccessibleFromGeneratedTopLevel(): Boolean {
  var current: KSDeclaration? = this
  while (current != null) {
    if (
      current.qualifiedName == null ||
        current.getVisibility() in setOf(Visibility.PRIVATE, Visibility.PROTECTED, Visibility.LOCAL)
    ) {
      return false
    }
    current = current.parentDeclaration
  }
  return true
}

private tailrec fun KSDeclaration.topLevelClass(): KSClassDeclaration =
  when (val parent = parentDeclaration) {
    null -> this as KSClassDeclaration
    else -> parent.topLevelClass()
  }
