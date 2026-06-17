// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnsafeCallOnNullableType")

package com.slack.circuit.subcircuit.codegen

import com.google.auto.service.AutoService
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getConstructors
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
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Visibility
import com.slack.circuit.subcircuit.codegen.SubCircuitCodegenMode.KOTLIN_INJECT_ANVIL
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import java.util.Locale
import kotlin.LazyThreadSafetyMode.NONE

@AutoService(SymbolProcessorProvider::class)
public class SubCircuitSymbolProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return SubCircuitSymbolProcessor(
      environment.logger,
      environment.codeGenerator,
      environment.options,
      environment.platforms,
    )
  }
}

private class SubCircuitSymbolProcessor(
  private val logger: KSPLogger,
  private val codeGenerator: CodeGenerator,
  options: Map<String, String>,
  private val platforms: List<PlatformInfo>,
) : SymbolProcessor {

  private val options = SubCircuitOptions.load(options, logger)
  private val mode = this.options.mode

  override fun process(resolver: Resolver): List<KSAnnotated> {
    if (options === SubCircuitOptions.UNKNOWN) return emptyList()
    val symbols = SubCircuitSymbols.create(resolver) ?: return emptyList()

    if (!options.mode.supportsPlatforms(platforms)) {
      logger.error("Unsupported platforms for codegen mode ${mode.name}. $platforms")
      return emptyList()
    }

    resolver
      .getSymbolsWithAnnotation(SubCircuitNames.SUB_CIRCUIT_INJECT_ANNOTATION.canonicalName)
      .forEach { annotatedElement ->
        when (annotatedElement) {
          is KSClassDeclaration ->
            generateFactory(annotatedElement, InstantiationType.CLASS, symbols)

          is KSFunctionDeclaration ->
            generateFactory(annotatedElement, InstantiationType.FUNCTION, symbols)

          else ->
            logger.error(
              "SubCircuitInject is only applicable on classes and functions.",
              annotatedElement,
            )
        }
      }
    return emptyList()
  }

  @Suppress("CyclomaticComplexMethod")
  private fun generateFactory(
    annotatedElement: KSAnnotated,
    instantiationType: InstantiationType,
    symbols: SubCircuitSymbols,
  ) {
    val subCircuitInjectAnnotation =
      annotatedElement
        .getKSAnnotationsWithLeniency(SubCircuitNames.SUB_CIRCUIT_INJECT_ANNOTATION)
        .single()

    // If we annotated a class, check that the class isn't using assisted inject. If so, error and
    // return
    if (instantiationType == InstantiationType.CLASS) {
      (annotatedElement as KSClassDeclaration).checkForAssistedInjection(mode) {
        return
      }
    }

    val screenKSType = subCircuitInjectAnnotation.arguments[0].value as KSType
    val screenIsObject =
      screenKSType.declaration.let { it is KSClassDeclaration && it.classKind == ClassKind.OBJECT }
    val screenType = screenKSType.toTypeName()
    val scope = (subCircuitInjectAnnotation.arguments[1].value as KSType).toTypeName()

    val factoryData =
      computeFactoryData(annotatedElement, symbols, screenKSType, instantiationType, logger, mode)
        ?: return

    val className =
      factoryData.className.replaceFirstChar { char ->
        char.takeIf { char.isLowerCase() }?.run { uppercase(Locale.US) } ?: char.toString()
      }
    val generatedClassName = className + factoryData.factoryType.suffix

    // Note: We can't directly reference the top-level class declaration for top-level functions in
    // kotlin. For annotatedElements which are top-level functions, topLevelClass will be null.
    val topLevelDeclaration = (annotatedElement as KSDeclaration).topLevelDeclaration()
    val topLevelClass = (topLevelDeclaration as? KSClassDeclaration)?.toClassName()

    val builder =
      TypeSpec.classBuilder(generatedClassName).apply {
        // Add the `@Inject` annotation to the appropriate place
        val constructorBuilder =
          FunSpec.constructorBuilder().addParameters(factoryData.constructorParams)
        mode.addInjectAnnotation(this, constructorBuilder, options)
        if (
          constructorBuilder.annotations.isNotEmpty() || constructorBuilder.parameters.isNotEmpty()
        ) {
          primaryConstructor(constructorBuilder.build())
        }

        if (factoryData.constructorParams.isNotEmpty()) {
          for (param in factoryData.constructorParams) {
            addProperty(
              PropertySpec.builder(param.name, param.type, KModifier.PRIVATE)
                .initializer(param.name)
                .build()
            )
          }
        }

        mode.annotateFactory(builder = this, scope = scope)
        val originAnnotation = mode.originAnnotation
        if (topLevelClass != null && originAnnotation != null) {
          addAnnotation(
            AnnotationSpec.builder(originAnnotation.className)
              .apply {
                val parameterName = originAnnotation.parameterName
                if (parameterName != null) {
                  addMember("%L = %T::class", parameterName, topLevelClass)
                } else {
                  addMember("%T::class", topLevelClass)
                }
              }
              .build()
          )
        }
        annotatedElement.qualifierAnnotation()?.let(::addAnnotation)
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
          builder.buildSubPresenterFactory(
            annotatedElement,
            screenBranch,
            factoryData.codeBlock,
            factoryData.hoistedBindings,
          )

        FactoryType.UI ->
          builder.buildSubUiFactory(
            annotatedElement,
            screenBranch,
            factoryData.codeBlock,
            factoryData.hoistedBindings,
          )
      }

    val originatingFile = listOfNotNull(annotatedElement.containingFile)

    FileSpec.get(factoryData.packageName, typeSpec)
      .writeTo(
        codeGenerator = codeGenerator,
        aggregating = false,
        originatingKSFiles = originatingFile,
      )

    val additionalType =
      mode.produceAdditionalTypeSpec(
        factoryType = factoryData.factoryType,
        factory = ClassName(factoryData.packageName, generatedClassName),
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

  private fun KSClassDeclaration.findConstructorAnnotatedWith(
    annotation: ClassName
  ): KSFunctionDeclaration? {
    return getConstructors().singleOrNull { constructor ->
      constructor.isAnnotationPresentWithLeniency(annotation)
    }
  }

  private inline fun KSClassDeclaration.checkForAssistedInjection(
    mode: SubCircuitCodegenMode,
    exit: () -> Nothing,
  ) {
    val assistedInjectClassName = mode.runtime.assistedInject ?: return
    // Check for an AssistedInject constructor
    if (findConstructorAnnotatedWith(assistedInjectClassName) != null) {
      val assistedFactory =
        declarations.filterIsInstance<KSClassDeclaration>().find { nestedClass ->
          mode.runtime.assistedFactory?.let { nestedClass.isAnnotationPresentWithLeniency(it) } ==
            true
        }
      val suffix =
        if (assistedFactory != null) " (${assistedFactory.qualifiedName?.asString()})" else ""
      logger.error(
        "When using @SubCircuitInject with an @AssistedInject-annotated class, you must " +
          "put the @SubCircuitInject annotation on the @AssistedFactory-annotated nested class$suffix.",
        this,
      )
      exit()
    }
  }

  private fun KSAnnotated.isAnnotationPresentWithLeniency(annotation: ClassName) =
    getKSAnnotationsWithLeniency(annotation).any()

  /**
   * Returns all annotations on this declaration that are meta-annotated with a known
   * [qualifier annotation][SubCircuitNames.QUALIFIER_ANNOTATION_NAMES], so they can be propagated
   * to the generated factory class.
   */
  private fun KSAnnotated.qualifierAnnotation(): AnnotationSpec? =
    annotations
      .firstOrNull { annotation ->
        annotation.annotationType.resolve().declaration.annotations.any { meta ->
          meta.annotationType.resolve().declaration.qualifiedName?.asString() in
            SubCircuitNames.QUALIFIER_ANNOTATION_NAMES
        }
      }
      ?.toAnnotationSpec()

  private fun KSAnnotated.getKSAnnotationsWithLeniency(
    annotation: ClassName
  ): Sequence<KSAnnotation> {
    val simpleName = annotation.simpleName
    return if (options.lenient) {
      annotations.filter { it.shortName.asString() == simpleName }
    } else {
      val qualifiedName = annotation.canonicalName
      this.annotations.filter {
        it.shortName.getShortName() == simpleName &&
          it.annotationType.resolve().declaration.qualifiedName?.asString() == qualifiedName
      }
    }
  }

  private data class FactoryData(
    val className: String,
    val packageName: String,
    val factoryType: FactoryType,
    val constructorParams: List<ParameterSpec>,
    val codeBlock: CodeBlock,
    /**
     * `val` bindings to emit inside the matching `when` branch of `create()`, before the final
     * instantiation expression. Used to hoist provider invocations out of the composable `SubUi`
     * lambda so they don't fire on every recomposition. Empty for presenters (their `create()` is
     * not composable).
     */
    val hoistedBindings: List<CodeBlock>,
  )

  /** Computes the data needed to generate a factory. */
  @Suppress("CyclomaticComplexMethod", "LongMethod")
  private fun computeFactoryData(
    annotatedElement: KSAnnotated,
    symbols: SubCircuitSymbols,
    screenKSType: KSType,
    instantiationType: InstantiationType,
    logger: KSPLogger,
    mode: SubCircuitCodegenMode,
  ): FactoryData? {
    val className: String
    val packageName: String
    val factoryType: FactoryType
    val constructorParams = mutableListOf<ParameterSpec>()
    val hoistedBindings = mutableListOf<CodeBlock>()
    val codeBlock: CodeBlock

    when (instantiationType) {
      InstantiationType.FUNCTION -> {
        // Functions are only supported for SubUi. SubCircuit has no `presenterOf` equivalent and
        // SubPresenter is not a fun interface, so function presenters are not supported.
        val fd = annotatedElement as KSFunctionDeclaration
        fd.checkVisibility(logger) {
          return null
        }
        val name = annotatedElement.simpleName.getShortName()
        className = name
        packageName = fd.packageName.asString()
        factoryType = FactoryType.UI

        // Any function parameter that isn't a circuit-provided type (state/modifier) is treated as
        // an injected dependency: added to the factory constructor as a provider (`Provider<T>` or
        // `() -> T`, depending on the mode) and invoked when the function is called.
        //
        // Exception: if the parameter type is already an indirect reference (Provider, Lazy, or a
        // Kotlin function type), we pass it through as-is without re-wrapping or invoking it.
        //
        // Non-pass-through providers are hoisted to a local `val` outside the SubUi lambda so
        // they're only invoked once per `create()` call, otherwise the composable block would
        // re-invoke the provider on every recomposition.
        val injectedParamBlocks = mutableListOf<CodeBlock>()
        for (param in fd.parameters) {
          val type = param.type.resolve()
          if (type.isSubCircuitProvidedParameter(symbols, factoryType)) continue
          val paramName = param.name!!.getShortName()
          if (type.isPassThroughProvider(mode)) {
            constructorParams.add(
              ParameterSpec.builder(paramName, type.toPassThroughTypeName()).build()
            )
            injectedParamBlocks.add(CodeBlock.of("%L·=·%L", paramName, paramName))
          } else {
            constructorParams.add(
              ParameterSpec.builder(paramName, mode.runtime.asProvider(type.toTypeName(), options))
                .build()
            )
            hoistedBindings.add(
              CodeBlock.of(
                "val·%L·=·%L",
                paramName,
                mode.runtime.getProviderBlock(CodeBlock.of(paramName)),
              )
            )
            injectedParamBlocks.add(CodeBlock.of("%L·=·%L", paramName, paramName))
          }
        }

        // State param is optional
        val stateParam =
          fd.parameters.singleOrNull { parameter ->
            symbols.subCircuitUiState.isAssignableFrom(parameter.type.resolve())
          }

        // Modifier param is required
        val modifierParam =
          fd.parameters.singleOrNull { parameter ->
            symbols.modifier.isAssignableFrom(parameter.type.resolve())
          }
            ?: run {
              logger.error("SubUi composable functions must have a Modifier parameter!", fd)
              return null
            }

        @Suppress("IfThenToElvis") // The elvis is less readable here
        val stateType =
          if (stateParam == null) SubCircuitNames.SUB_CIRCUIT_UI_STATE
          else stateParam.type.resolve().toTypeName()
        val stateArg = if (stateParam == null) "_" else "state"
        val stateParamBlock =
          if (stateParam == null) CodeBlock.of("")
          else CodeBlock.of("%L·=·state,·", stateParam.name!!.getShortName())
        val modifierParamBlock = CodeBlock.of("%L·=·modifier", modifierParam.name!!.getShortName())
        val injectedBlock =
          if (injectedParamBlocks.isEmpty()) {
            CodeBlock.of("")
          } else {
            CodeBlock.of(",·%L", injectedParamBlocks.joinToCode(",·"))
          }
        codeBlock =
          CodeBlock.of(
            "%T<%T>·{·%L,·modifier·->·%M(%L%L%L)·}",
            SubCircuitNames.SUB_UI,
            stateType,
            stateArg,
            MemberName(packageName, name),
            stateParamBlock,
            modifierParamBlock,
            injectedBlock,
          )
      }

      InstantiationType.CLASS -> {
        val declaration = annotatedElement as KSClassDeclaration
        declaration.checkVisibility(logger) {
          return null
        }
        val injectableConstructor by
          lazy(NONE) {
            declaration.findConstructorAnnotatedWith(mode.runtime.inject(options))
              ?: declaration.primaryConstructor
          }
        val assistedKSParams by
          lazy(NONE) {
            injectableConstructor
              ?.parameters
              ?.filter { it.isAnnotationPresentWithLeniency(mode.runtime.assisted) }
              .orEmpty()
          }

        val isAssisted =
          assistedKSParams.isNotEmpty() ||
            mode.runtime.assistedFactory?.let { declaration.isAnnotationPresentWithLeniency(it) } ==
              true

        val creatorOrConstructor: KSFunctionDeclaration?
        val targetClass: KSClassDeclaration
        if (isAssisted) {
          if (mode == KOTLIN_INJECT_ANVIL) {
            creatorOrConstructor = injectableConstructor
            targetClass = declaration
          } else {
            val creatorFunction = declaration.getAllFunctions().filter { it.isAbstract }.single()
            creatorOrConstructor = creatorFunction
            targetClass = creatorFunction.returnType!!.resolve().declaration as KSClassDeclaration
            targetClass.checkVisibility(logger) {
              return null
            }
          }
        } else {
          creatorOrConstructor = injectableConstructor
          targetClass = declaration
        }
        val useProvider =
          !isAssisted &&
            mode.filterValidInjectionSites(listOfNotNull(creatorOrConstructor, declaration)).any {
              injectionSite ->
              mode.runtime.declarationInjects(options).any { annotation ->
                injectionSite.isAnnotationPresentWithLeniency(annotation)
              }
            }
        if (!isAssisted && !useProvider) {
          logger.error(
            "@SubCircuitInject-annotated classes must be injectable: annotate the class or a " +
              "constructor with @Inject, or annotate an @AssistedFactory interface.",
            declaration,
          )
          return null
        }
        // For the assisted path we name the generated factory after the annotated @AssistedFactory
        // interface (e.g. TestPresenter.Factory -> TestPresenter_Factory_SubPresenterFactory). For
        // the provider/KI path we name it after the target class.
        className =
          if (isAssisted && mode != KOTLIN_INJECT_ANVIL) {
            declaration.toClassName().simpleNames.joinToString("_")
          } else {
            targetClass.simpleName.getShortName()
          }
        packageName = declaration.packageName.asString()
        factoryType =
          targetClass
            .getAllSuperTypes()
            .mapNotNull {
              when (it.declaration.qualifiedName?.asString()) {
                SubCircuitNames.SUB_UI.canonicalName -> FactoryType.UI
                SubCircuitNames.SUB_PRESENTER.canonicalName -> FactoryType.PRESENTER
                else -> null
              }
            }
            .firstOrNull()
            ?: run {
              logger.error(
                "Factory must be for a SubUi or SubPresenter class, but was " +
                  "${targetClass.qualifiedName?.asString()}.",
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
              // Don't include parameter names if a factory lambda is going to be used.
              includeParameterNames = !(isAssisted && mode == KOTLIN_INJECT_ANVIL),
            ) ?: CodeBlock.of("")
          }
        codeBlock =
          if (useProvider) {
            // Inject a Provider<TargetClass> that we'll call get() on.
            constructorParams.add(
              ParameterSpec.builder(
                  "provider",
                  mode.runtime.asProvider(targetClass.toClassName(), options),
                )
                .build()
            )
            mode.runtime.getProviderBlock(CodeBlock.of("provider"))
          } else {
            // isAssisted: inject the target class's assisted factory and call create() on it.
            when (mode) {
              KOTLIN_INJECT_ANVIL -> {
                val factoryLambda =
                  LambdaTypeName.get(
                    receiver = null,
                    parameters =
                      assistedKSParams.map { ksParam ->
                        ParameterSpec.builder(
                            ksParam.name!!.getShortName(),
                            ksParam.type.toTypeName(),
                          )
                          .build()
                      },
                    returnType = targetClass.toClassName(),
                  )
                constructorParams.add(ParameterSpec.builder("factory", factoryLambda).build())
                CodeBlock.of("factory(%L)", assistedParams)
              }
              else -> {
                constructorParams.add(
                  ParameterSpec.builder("factory", declaration.toClassName()).build()
                )
                CodeBlock.of(
                  "factory.%L(%L)",
                  creatorOrConstructor!!.simpleName.getShortName(),
                  assistedParams,
                )
              }
            }
          }
      }
    }
    return FactoryData(
      className,
      packageName,
      factoryType,
      constructorParams,
      codeBlock,
      hoistedBindings,
    )
  }
}

private data class AssistedType(
  val factoryName: String,
  val type: TypeName,
  val name: String,
  val includeExplicitCast: Boolean = false,
)

/**
 * Returns a [CodeBlock] representation of all named assisted parameters on this
 * [KSFunctionDeclaration] to be used in generated invocation code.
 *
 * For SubCircuit, the only circuit-provided assisted parameter is the screen. If [screenType] is an
 * object, then the CodeBlock will include an explicit cast to the type since objects are checked by
 * equality and don't benefit from smart casts.
 *
 * Example: the create method `fun create(screen: MyScreen): MyPresenter` yields the CodeBlock
 * `screen = screen`.
 */
private fun KSFunctionDeclaration.assistedParameters(
  symbols: SubCircuitSymbols,
  logger: KSPLogger,
  screenType: KSType,
  includeParameterNames: Boolean,
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
        if (type.isSubScreenType()) {
          if (screenType.isSameDeclarationAs(type)) {
            val screenIsObject =
              screenType.declaration.let {
                it is KSClassDeclaration && it.classKind == ClassKind.OBJECT
              }
            addOrError(
              AssistedType(
                factoryName = "screen",
                type = type.toTypeName(),
                name = param.name!!.getShortName(),
                includeExplicitCast = screenIsObject,
              )
            )
          } else {
            logger.error("Screen type mismatch. Expected $screenType but found $type", param)
          }
        }
      }
    }
    .toList()
    .map {
      val prefix =
        if (includeParameterNames) {
          "${it.name} = "
        } else {
          ""
        }
      if (it.includeExplicitCast) {
        CodeBlock.of("$prefix${it.factoryName}·as·%T", it.type)
      } else {
        CodeBlock.of("$prefix${it.factoryName}")
      }
    }
    .joinToCode(",·")
}

private fun KSType.isSameDeclarationAs(type: KSType): Boolean {
  return this.declaration == type.declaration
}

private fun KSType.isInstanceOf(type: KSType): Boolean {
  return type.isAssignableFrom(this)
}

/**
 * Whether this type is (or extends) [SubScreen]. [SubScreen] is generic, so a plain
 * `isAssignableFrom` against the raw declaration is unreliable; instead we walk the declaration's
 * supertypes by qualified name.
 */
private fun KSType.isSubScreenType(): Boolean {
  val declaration = declaration as? KSClassDeclaration ?: return false
  if (declaration.qualifiedName?.asString() == SubCircuitNames.SUB_SCREEN.canonicalName) return true
  return declaration.getAllSuperTypes().any {
    it.declaration.qualifiedName?.asString() == SubCircuitNames.SUB_SCREEN.canonicalName
  }
}

/** Whether this type matches one of the parameter types that SubCircuit provides for a function. */
private fun KSType.isSubCircuitProvidedParameter(
  symbols: SubCircuitSymbols,
  factoryType: FactoryType,
): Boolean {
  return when (factoryType) {
    FactoryType.PRESENTER -> isSubScreenType()
    FactoryType.UI -> isInstanceOf(symbols.subCircuitUiState) || isInstanceOf(symbols.modifier)
  }
}

/**
 * Whether this type is already an "indirect" reference to a dependency (a provider, a `Lazy`, or
 * `() -> T`). Such types are passed through to the generated factory as-is rather than being
 * re-wrapped in a provider and invoked at `create()` time.
 *
 * `kotlin.Function0` is only treated as a provider for modes whose runtimes natively understand it
 * as one, currently Metro and kotlin-inject. For Dagger/Anvil/Hilt it falls through to the normal
 * "wrap in a Provider" path.
 */
private fun KSType.isPassThroughProvider(mode: SubCircuitCodegenMode): Boolean {
  val qName = declaration.qualifiedName?.asString() ?: return false
  if (qName !in SubCircuitNames.PASS_THROUGH_PROVIDER_NAMES) return false
  return qName != "kotlin.Function0" ||
    mode == SubCircuitCodegenMode.METRO ||
    mode == KOTLIN_INJECT_ANVIL
}

/**
 * Like [KSType.toTypeName], but renders `kotlin.Function0<T>` as its lambda form (`() -> T`) so it
 * appears naturally in generated sources.
 */
private fun KSType.toPassThroughTypeName(): TypeName {
  if (declaration.qualifiedName?.asString() != "kotlin.Function0") return toTypeName()
  val returnType = arguments.singleOrNull()?.type?.resolve()?.toTypeName() ?: return toTypeName()
  return LambdaTypeName.get(returnType = returnType)
}

private fun TypeSpec.Builder.buildSubUiFactory(
  originatingSymbol: KSAnnotated,
  screenBranch: CodeBlock,
  instantiationCodeBlock: CodeBlock,
  hoistedBindings: List<CodeBlock>,
): TypeSpec {
  return addSuperinterface(SubCircuitNames.SUB_UI_FACTORY)
    .addFunction(
      FunSpec.builder("create")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("screen", SubCircuitNames.SUB_SCREEN.parameterizedBy(STAR))
        .returns(SubCircuitNames.SUB_UI.parameterizedBy(STAR).copy(nullable = true))
        .beginControlFlow("return·when·(screen)")
        .addWhenBranch(screenBranch, instantiationCodeBlock, hoistedBindings)
        .addStatement("else·->·null")
        .endControlFlow()
        .build()
    )
    .addOriginatingKSFile(originatingSymbol.containingFile!!)
    .build()
}

private fun TypeSpec.Builder.buildSubPresenterFactory(
  originatingSymbol: KSAnnotated,
  screenBranch: CodeBlock,
  instantiationCodeBlock: CodeBlock,
  hoistedBindings: List<CodeBlock>,
): TypeSpec {
  // The TypeSpec below will generate something similar to the following.
  //  public class MyPresenter_Factory_SubPresenterFactory(
  //    private val factory: MyPresenter.Factory,
  //  ) : SubPresenterFactory {
  //    public override fun create(screen: SubScreen<*>): SubPresenter<*, *>? = when (screen) {
  //      is MyScreen -> factory.create(screen = screen)
  //      else -> null
  //    }
  //  }

  return addSuperinterface(SubCircuitNames.SUB_PRESENTER_FACTORY)
    .addFunction(
      FunSpec.builder("create")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("screen", SubCircuitNames.SUB_SCREEN.parameterizedBy(STAR))
        .returns(SubCircuitNames.SUB_PRESENTER.parameterizedBy(STAR, STAR).copy(nullable = true))
        .beginControlFlow("return when (screen)")
        .addWhenBranch(screenBranch, instantiationCodeBlock, hoistedBindings)
        .addStatement("else·->·null")
        .endControlFlow()
        .build()
    )
    .addOriginatingKSFile(originatingSymbol.containingFile!!)
    .build()
}

/**
 * Emits a single branch of the `when (screen)` block. If [hoistedBindings] is empty it emits a
 * simple `is Screen -> expr` line; otherwise it emits a block-bodied branch that declares each
 * `val` binding before the final expression, so any provider invocations happen once per `create()`
 * call rather than on every recomposition.
 */
private fun FunSpec.Builder.addWhenBranch(
  screenBranch: CodeBlock,
  instantiationCodeBlock: CodeBlock,
  hoistedBindings: List<CodeBlock>,
): FunSpec.Builder {
  if (hoistedBindings.isEmpty()) {
    addStatement("%L·->·%L", screenBranch, instantiationCodeBlock)
  } else {
    beginControlFlow("%L·->", screenBranch)
    for (binding in hoistedBindings) {
      addStatement("%L", binding)
    }
    addStatement("%L", instantiationCodeBlock)
    endControlFlow()
  }
  return this
}

private val FactoryType.suffix: String
  get() =
    when (this) {
      FactoryType.PRESENTER -> SubCircuitNames.SUB_PRESENTER_FACTORY_SUFFIX
      FactoryType.UI -> SubCircuitNames.SUB_UI_FACTORY_SUFFIX
    }

private inline fun KSDeclaration.checkVisibility(logger: KSPLogger, returnBody: () -> Unit) {
  if (!getVisibility().isVisible) {
    logger.error(
      "SubCircuitInject is not applicable to private or local functions and classes.",
      this,
    )
    returnBody()
  }
}

private fun KSDeclaration.topLevelDeclaration(): KSDeclaration {
  return parentDeclaration?.topLevelDeclaration() ?: this
}

private val Visibility.isVisible: Boolean
  get() = this != Visibility.PRIVATE && this != Visibility.LOCAL
