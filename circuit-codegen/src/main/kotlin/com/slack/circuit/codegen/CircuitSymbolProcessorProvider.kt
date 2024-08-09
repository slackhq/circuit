// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnsafeCallOnNullableType")

package com.slack.circuit.codegen

import com.google.auto.service.AutoService
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.JvmPlatformInfo
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
import com.squareup.kotlinpoet.AnnotationSpec
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
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import java.util.Locale

private val ASSISTED_FACTORY = ClassName("dagger.assisted", "AssistedFactory")
private val ASSISTED_INJECT = ClassName("dagger.assisted", "AssistedInject")
private val INJECT = ClassName("javax.inject", "Inject")
private val PROVIDER = ClassName("javax.inject", "Provider")
private const val CIRCUIT_RUNTIME_BASE_PACKAGE = "com.slack.circuit.runtime"
private const val DAGGER_PACKAGE = "dagger"
private const val DAGGER_HILT_PACKAGE = "$DAGGER_PACKAGE.hilt"
private const val DAGGER_HILT_CODEGEN_PACKAGE = "$DAGGER_HILT_PACKAGE.codegen"
private const val DAGGER_MULTIBINDINGS_PACKAGE = "$DAGGER_PACKAGE.multibindings"
private const val CIRCUIT_RUNTIME_UI_PACKAGE = "$CIRCUIT_RUNTIME_BASE_PACKAGE.ui"
private const val CIRCUIT_RUNTIME_SCREEN_PACKAGE = "$CIRCUIT_RUNTIME_BASE_PACKAGE.screen"
private const val CIRCUIT_RUNTIME_PRESENTER_PACKAGE = "$CIRCUIT_RUNTIME_BASE_PACKAGE.presenter"
private val MODIFIER = ClassName("androidx.compose.ui", "Modifier")
private val CIRCUIT_INJECT_ANNOTATION =
  ClassName("com.slack.circuit.codegen.annotations", "CircuitInject")
private val CIRCUIT_PRESENTER = ClassName(CIRCUIT_RUNTIME_PRESENTER_PACKAGE, "Presenter")
private val CIRCUIT_PRESENTER_FACTORY = CIRCUIT_PRESENTER.nestedClass("Factory")
private val CIRCUIT_UI = ClassName(CIRCUIT_RUNTIME_UI_PACKAGE, "Ui")
private val CIRCUIT_UI_FACTORY = CIRCUIT_UI.nestedClass("Factory")
private val CIRCUIT_UI_STATE = ClassName(CIRCUIT_RUNTIME_BASE_PACKAGE, "CircuitUiState")
private val SCREEN = ClassName(CIRCUIT_RUNTIME_SCREEN_PACKAGE, "Screen")
private val NAVIGATOR = ClassName(CIRCUIT_RUNTIME_BASE_PACKAGE, "Navigator")
private val CIRCUIT_CONTEXT = ClassName(CIRCUIT_RUNTIME_BASE_PACKAGE, "CircuitContext")
private val DAGGER_MODULE = ClassName(DAGGER_PACKAGE, "Module")
private val DAGGER_BINDS = ClassName(DAGGER_PACKAGE, "Binds")
private val DAGGER_INSTALL_IN = ClassName(DAGGER_HILT_PACKAGE, "InstallIn")
private val DAGGER_ORIGINATING_ELEMENT =
  ClassName(DAGGER_HILT_CODEGEN_PACKAGE, "OriginatingElement")
private val DAGGER_INTO_SET = ClassName(DAGGER_MULTIBINDINGS_PACKAGE, "IntoSet")
private const val MODULE = "Module"
private const val FACTORY = "Factory"
private const val CIRCUIT_CODEGEN_MODE = "circuit.codegen.mode"

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

private class CircuitSymbols private constructor(resolver: Resolver) {
  val modifier = resolver.loadKSType(MODIFIER.canonicalName)
  val circuitUiState = resolver.loadKSType(CIRCUIT_UI_STATE.canonicalName)
  val screen = resolver.loadKSType(SCREEN.canonicalName)
  val navigator = resolver.loadKSType(NAVIGATOR.canonicalName)

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

private fun Resolver.loadKSType(name: String): KSType =
  loadOptionalKSType(name) ?: error("Could not find $name in classpath")

private fun Resolver.loadOptionalKSType(name: String?): KSType? {
  if (name == null) return null
  return getClassDeclarationByName(getKSNameFromString(name))?.asType(emptyList())
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
      options[CIRCUIT_CODEGEN_MODE].let { mode ->
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

    resolver.getSymbolsWithAnnotation(CIRCUIT_INJECT_ANNOTATION.canonicalName).forEach {
      annotatedElement ->
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
      annotatedElement.getKSAnnotationsWithLeniency(CIRCUIT_INJECT_ANNOTATION).single()

    // If we annotated a class, check that the class isn't using assisted inject. If so, error and
    // return
    if (instantiationType == InstantiationType.CLASS) {
      (annotatedElement as KSClassDeclaration).checkForAssistedInjection {
        return
      }
    }

    val screenKSType = circuitInjectAnnotation.arguments[0].value as KSType
    val screenIsObject =
      screenKSType.declaration.let { it is KSClassDeclaration && it.classKind == ClassKind.OBJECT }
    val screenType = screenKSType.toTypeName()
    val scope = (circuitInjectAnnotation.arguments[1].value as KSType).toTypeName()

    val factoryData =
      computeFactoryData(annotatedElement, symbols, screenKSType, instantiationType, logger)
        ?: return

    val className =
      factoryData.className.replaceFirstChar { char ->
        char.takeIf { char.isLowerCase() }?.run { uppercase(Locale.US) } ?: char.toString()
      }

    val builder =
      TypeSpec.classBuilder(className + FACTORY)
        .primaryConstructor(
          FunSpec.constructorBuilder()
            .addAnnotation(INJECT)
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
        factory = ClassName(factoryData.packageName, className + FACTORY),
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

  private inline fun KSClassDeclaration.checkForAssistedInjection(exit: () -> Nothing) {
    // Check for an AssistedInject constructor
    if (findConstructorAnnotatedWith(ASSISTED_INJECT) != null) {
      val assistedFactory =
        declarations.filterIsInstance<KSClassDeclaration>().find {
          it.isAnnotationPresentWithLeniency(ASSISTED_FACTORY)
        }
      val suffix =
        if (assistedFactory != null) " (${assistedFactory.qualifiedName?.asString()})" else ""
      logger.error(
        "When using @CircuitInject with an @AssistedInject-annotated class, you must " +
          "put the @CircuitInject annotation on the @AssistedFactory-annotated nested class$suffix.",
        this,
      )
      exit()
    }
  }

  private fun KSAnnotated.isAnnotationPresentWithLeniency(annotation: ClassName) =
    getKSAnnotationsWithLeniency(annotation).any()

  private fun KSAnnotated.getKSAnnotationsWithLeniency(
    annotation: ClassName
  ): Sequence<KSAnnotation> {
    val simpleName = annotation.simpleName
    return if (lenient) {
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
          fd.assistedParameters(symbols, logger, screenKSType, factoryType == FactoryType.PRESENTER)
        codeBlock =
          when (factoryType) {
            FactoryType.PRESENTER ->
              CodeBlock.of(
                "%M·{·%M(%L)·}",
                MemberName(CIRCUIT_RUNTIME_PRESENTER_PACKAGE, "presenterOf"),
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
                if (stateParam == null) CIRCUIT_UI_STATE else stateParam.type.resolve().toTypeName()
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
                MemberName(CIRCUIT_RUNTIME_UI_PACKAGE, "ui"),
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
        val isAssisted = declaration.isAnnotationPresentWithLeniency(ASSISTED_FACTORY)
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
          !isAssisted && creatorOrConstructor?.isAnnotationPresentWithLeniency(INJECT) == true
        className = targetClass.simpleName.getShortName()
        packageName = targetClass.packageName.asString()
        factoryType =
          targetClass
            .getAllSuperTypes()
            .mapNotNull {
              when (it.declaration.qualifiedName?.asString()) {
                CIRCUIT_UI.canonicalName -> FactoryType.UI
                CIRCUIT_PRESENTER.canonicalName -> FactoryType.PRESENTER
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
              symbols,
              logger,
              screenKSType,
              allowNavigator = factoryType == FactoryType.PRESENTER,
            )
          }
        codeBlock =
          if (useProvider) {
            // Inject a Provider<TargetClass> that we'll call get() on.
            constructorParams.add(
              ParameterSpec.builder("provider", PROVIDER.parameterizedBy(targetClass.toClassName()))
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
            // Simple constructor call, no injection.
            CodeBlock.of("%T(%L)", targetClass.toClassName(), assistedParams)
          }
      }
    }
    return FactoryData(className, packageName, factoryType, constructorParams, codeBlock)
  }
}

private data class AssistedType(val factoryName: String, val type: TypeName, val name: String)

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
private fun KSFunctionDeclaration.assistedParameters(
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

private fun KSType.isSameDeclarationAs(type: KSType): Boolean {
  return this.declaration == type.declaration
}

private fun KSType.isInstanceOf(type: KSType): Boolean {
  return type.isAssignableFrom(this)
}

private fun TypeSpec.Builder.buildUiFactory(
  originatingSymbol: KSAnnotated,
  screenBranch: CodeBlock,
  instantiationCodeBlock: CodeBlock,
): TypeSpec {
  return addSuperinterface(CIRCUIT_UI_FACTORY)
    .addFunction(
      FunSpec.builder("create")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("screen", SCREEN)
        .addParameter("context", CIRCUIT_CONTEXT)
        .returns(CIRCUIT_UI.parameterizedBy(STAR).copy(nullable = true))
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

  return addSuperinterface(CIRCUIT_PRESENTER_FACTORY)
    .addFunction(
      FunSpec.builder("create")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("screen", SCREEN)
        .addParameter("navigator", NAVIGATOR)
        .addParameter("context", CIRCUIT_CONTEXT)
        .returns(CIRCUIT_PRESENTER.parameterizedBy(STAR).copy(nullable = true))
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
  UI,
}

private enum class InstantiationType {
  FUNCTION,
  CLASS,
}

private enum class CodegenMode {
  /**
   * The Anvil Codegen mode
   *
   * This mode annotates generated factory types with [ContributesMultibinding], allowing for Anvil
   * to automatically wire the generated class up to Dagger's multibinding system within a given
   * scope (e.g. AppScope).
   *
   * ```kotlin
   * @ContributesMultibinding(AppScope::class)
   * public class FavoritesPresenterFactory @Inject constructor(
   *   private val factory: FavoritesPresenter.Factory,
   * ) : Presenter.Factory { ... }
   * ```
   */
  ANVIL {
    private val contributesMultibindingCN =
      ClassName("com.squareup.anvil.annotations", "ContributesMultibinding")

    override fun supportsPlatforms(platforms: List<PlatformInfo>): Boolean {
      // Anvil only supports JVM & Android
      return platforms.all { it is JvmPlatformInfo }
    }

    override fun annotateFactory(builder: TypeSpec.Builder, scope: TypeName) {
      builder.addAnnotation(
        AnnotationSpec.builder(contributesMultibindingCN).addMember("%T::class", scope).build()
      )
    }
  },

  /**
   * The Hilt Codegen mode
   *
   * This mode provides an additional type, a Hilt module, which binds the generated factory, wiring
   * up multibinding in the Hilt DI framework. The scope provided via [CircuitInject] is used to
   * define the dagger component the factory provider is installed in.
   *
   * ```kotlin
   * @Module
   * @InstallIn(SingletonComponent::class)
   * @OriginatingElement(topLevelClass = FavoritesPresenter::class)
   * public abstract class FavoritesPresenterFactoryModule {
   *   @Binds
   *   @IntoSet
   *   public abstract
   *       fun bindFavoritesPresenterFactory(favoritesPresenterFactory: FavoritesPresenterFactory):
   *       Presenter.Factory
   * }
   * ```
   */
  HILT {
    override fun supportsPlatforms(platforms: List<PlatformInfo>): Boolean {
      // Hilt only supports JVM & Android
      return platforms.all { it is JvmPlatformInfo }
    }

    override fun produceAdditionalTypeSpec(
      factory: ClassName,
      factoryType: FactoryType,
      scope: TypeName,
      topLevelClass: ClassName?,
    ): TypeSpec {
      val moduleAnnotations =
        listOfNotNull(
          AnnotationSpec.builder(DAGGER_MODULE).build(),
          AnnotationSpec.builder(DAGGER_INSTALL_IN).addMember("%T::class", scope).build(),
          topLevelClass?.let {
            AnnotationSpec.builder(DAGGER_ORIGINATING_ELEMENT)
              .addMember("%L = %T::class", "topLevelClass", topLevelClass)
              .build()
          },
        )

      val providerAnnotations =
        listOf(
          AnnotationSpec.builder(DAGGER_BINDS).build(),
          AnnotationSpec.builder(DAGGER_INTO_SET).build(),
        )

      val providerReturns =
        if (factoryType == FactoryType.UI) {
          CIRCUIT_UI_FACTORY
        } else {
          CIRCUIT_PRESENTER_FACTORY
        }

      val factoryName = factory.simpleName

      val providerSpec =
        FunSpec.builder("bind${factoryName}")
          .addModifiers(KModifier.ABSTRACT)
          .addAnnotations(providerAnnotations)
          .addParameter(name = factoryName.replaceFirstChar { it.lowercase() }, type = factory)
          .returns(providerReturns)
          .build()

      return TypeSpec.classBuilder(factory.peerClass(factoryName + MODULE))
        .addModifiers(KModifier.ABSTRACT)
        .addAnnotations(moduleAnnotations)
        .addFunction(providerSpec)
        .build()
    }
  };

  open fun annotateFactory(builder: TypeSpec.Builder, scope: TypeName) {}

  open fun produceAdditionalTypeSpec(
    factory: ClassName,
    factoryType: FactoryType,
    scope: TypeName,
    topLevelClass: ClassName?,
  ): TypeSpec? {
    return null
  }

  abstract fun supportsPlatforms(platforms: List<PlatformInfo>): Boolean
}

private inline fun KSDeclaration.checkVisibility(logger: KSPLogger, returnBody: () -> Unit) {
  if (!getVisibility().isVisible) {
    logger.error("CircuitInject is not applicable to private or local functions and classes.", this)
    returnBody()
  }
}

private fun KSDeclaration.topLevelDeclaration(): KSDeclaration {
  return parentDeclaration?.topLevelDeclaration() ?: this
}

private val Visibility.isVisible: Boolean
  get() = this != Visibility.PRIVATE && this != Visibility.LOCAL
