// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit.codegen

import com.google.devtools.ksp.processing.JvmPlatformInfo
import com.google.devtools.ksp.processing.PlatformInfo
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.slack.circuit.subcircuit.codegen.FactoryType.UI
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal enum class SubCircuitCodegenMode {
  UNKNOWN {
    override fun supportsPlatforms(platforms: List<PlatformInfo>): Boolean {
      return false
    }

    override fun addInjectAnnotation(
      classBuilder: TypeSpec.Builder,
      constructorBuilder: FunSpec.Builder,
      options: SubCircuitOptions,
    ) {
      error("Unknown codegen mode, should not be called")
    }
  },
  /**
   * The Anvil Codegen mode
   *
   * This mode annotates generated factory types with `ContributesMultibinding`, allowing for Anvil
   * to automatically wire the generated class up to Dagger's multibinding system within a given
   * scope (e.g. AppScope).
   *
   * ```kotlin
   * @ContributesMultibinding(AppScope::class)
   * public class MyPresenter_Factory_SubPresenterFactory @Inject constructor(
   *   private val factory: MyPresenter.Factory,
   * ) : SubPresenterFactory { ... }
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

    override fun addInjectAnnotation(
      classBuilder: TypeSpec.Builder,
      constructorBuilder: FunSpec.Builder,
      options: SubCircuitOptions,
    ) {
      constructorBuilder.addAnnotation(runtime.inject(options))
    }
  },

  /**
   * The Hilt Codegen mode
   *
   * This mode provides an additional type, a Hilt module, which binds the generated factory, wiring
   * up multibinding in the Hilt DI framework. The scope provided via `SubCircuitInject` is used to
   * define the dagger component the factory provider is installed in.
   *
   * ```kotlin
   * @Module
   * @InstallIn(SingletonComponent::class)
   * @OriginatingElement(topLevelClass = MyPresenter::class)
   * public abstract class MyPresenter_Factory_SubPresenterFactoryModule {
   *   @Binds
   *   @IntoSet
   *   public abstract
   *       fun bindMyPresenter_Factory_SubPresenterFactory(
   *           myPresenter_Factory_SubPresenterFactory: MyPresenter_Factory_SubPresenterFactory):
   *       SubPresenterFactory
   * }
   * ```
   */
  HILT {
    override val originAnnotation: OriginAnnotation = SubCircuitNames.DAGGER_ORIGIN

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
          AnnotationSpec.builder(SubCircuitNames.DAGGER_MODULE).build(),
          AnnotationSpec.builder(SubCircuitNames.DAGGER_INSTALL_IN)
            .addMember("%T::class", scope)
            .build(),
          // Required to generate this here too since this is a separate class from the generated
          // factory
          topLevelClass?.let {
            AnnotationSpec.builder(SubCircuitNames.DAGGER_ORIGINATING_ELEMENT)
              .addMember("%L = %T::class", "topLevelClass", topLevelClass)
              .build()
          },
        )

      val providerAnnotations =
        listOf(
          AnnotationSpec.builder(SubCircuitNames.DAGGER_BINDS).build(),
          AnnotationSpec.builder(SubCircuitNames.DAGGER_INTO_SET).build(),
        )

      val providerReturns =
        if (factoryType == UI) {
          SubCircuitNames.SUB_UI_FACTORY
        } else {
          SubCircuitNames.SUB_PRESENTER_FACTORY
        }

      val factoryName = factory.simpleName

      val providerSpec =
        FunSpec.builder("bind${factoryName}")
          .addModifiers(ABSTRACT)
          .addAnnotations(providerAnnotations)
          .addParameter(name = factoryName.replaceFirstChar { it.lowercase() }, type = factory)
          .returns(providerReturns)
          .build()

      return TypeSpec.classBuilder(factory.peerClass(factoryName + SubCircuitNames.MODULE))
        .addModifiers(ABSTRACT)
        .addAnnotations(moduleAnnotations)
        .addFunction(providerSpec)
        .build()
    }

    override fun addInjectAnnotation(
      classBuilder: TypeSpec.Builder,
      constructorBuilder: FunSpec.Builder,
      options: SubCircuitOptions,
    ) {
      constructorBuilder.addAnnotation(runtime.inject(options))
    }
  },

  /**
   * The `kotlin-inject` Anvil Codegen mode
   *
   * This mode annotates generated factory types with `ContributesBinding`, allowing for KI-Anvil to
   * automatically wire the generated class up to KI's multibinding system within a given scope
   * (e.g. AppScope).
   *
   * ```kotlin
   * @Inject
   * @ContributesBinding(AppScope::class, multibinding = true)
   * public class MyPresenter_SubPresenterFactory(
   *   private val provider: () -> MyPresenter,
   * ) : SubPresenterFactory { ... }
   * ```
   */
  KOTLIN_INJECT_ANVIL {
    override val runtime: InjectionRuntime = InjectionRuntime.KotlinInject
    override val originAnnotation: OriginAnnotation = SubCircuitNames.KotlinInject.Anvil.ORIGIN

    override fun supportsPlatforms(platforms: List<PlatformInfo>): Boolean {
      // KI-Anvil supports all
      return true
    }

    override fun annotateFactory(builder: TypeSpec.Builder, scope: TypeName) {
      builder.addAnnotation(
        AnnotationSpec.builder(SubCircuitNames.KotlinInject.Anvil.CONTRIBUTES_BINDING)
          .addMember("%T::class", scope)
          .addMember("multibinding = true")
          .build()
      )
    }

    override fun addInjectAnnotation(
      classBuilder: TypeSpec.Builder,
      constructorBuilder: FunSpec.Builder,
      options: SubCircuitOptions,
    ) {
      classBuilder.addAnnotation(runtime.inject(options))
    }

    override fun filterValidInjectionSites(
      candidates: Collection<KSDeclaration>
    ): Collection<KSDeclaration> {
      return candidates.filter { it is KSFunctionDeclaration || it is KSClassDeclaration }
    }
  },

  /**
   * The `metro` code gen mode
   *
   * This mode annotates generated factory types with `ContributesIntoSet`, allowing for Metro to
   * automatically wire the generated class up to its multibinding system within a given scope (e.g.
   * AppScope).
   *
   * ```kotlin
   * @Inject
   * @ContributesIntoSet(AppScope::class)
   * public class MyPresenter_SubPresenterFactory(
   *   private val provider: Provider<MyPresenter>
   * ) : SubPresenterFactory { ... }
   * ```
   */
  METRO {
    override val runtime: InjectionRuntime = InjectionRuntime.Metro
    override val originAnnotation: OriginAnnotation = SubCircuitNames.Metro.ORIGIN

    override fun supportsPlatforms(platforms: List<PlatformInfo>): Boolean {
      // Metro supports all
      return true
    }

    override fun annotateFactory(builder: TypeSpec.Builder, scope: TypeName) {
      builder.addAnnotation(
        AnnotationSpec.builder(SubCircuitNames.Metro.CONTRIBUTES_INTO_SET)
          .addMember("%T::class", scope)
          .build()
      )
    }

    override fun addInjectAnnotation(
      classBuilder: TypeSpec.Builder,
      constructorBuilder: FunSpec.Builder,
      options: SubCircuitOptions,
    ) {
      classBuilder.addAnnotation(runtime.inject(options))
    }

    override fun filterValidInjectionSites(
      candidates: Collection<KSDeclaration>
    ): Collection<KSDeclaration> {
      return candidates.filter { it is KSFunctionDeclaration || it is KSClassDeclaration }
    }
  };

  open val runtime: InjectionRuntime = InjectionRuntime.Jakarta
  open val originAnnotation: OriginAnnotation? = null

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

  abstract fun addInjectAnnotation(
    classBuilder: TypeSpec.Builder,
    constructorBuilder: FunSpec.Builder,
    options: SubCircuitOptions,
  )

  /** Filters the candidates for @Inject annotation placement. */
  open fun filterValidInjectionSites(
    candidates: Collection<KSDeclaration>
  ): Collection<KSDeclaration> = candidates.filterIsInstance<KSFunctionDeclaration>()

  sealed interface InjectionRuntime {

    fun inject(options: SubCircuitOptions): ClassName

    fun declarationInjects(options: SubCircuitOptions): Collection<ClassName> =
      listOf(inject(options))

    val assisted: ClassName
    val assistedInject: ClassName?
    val assistedFactory: ClassName?

    fun asProvider(providedType: TypeName, options: SubCircuitOptions): TypeName

    fun getProviderBlock(provider: CodeBlock): CodeBlock

    data object Jakarta : InjectionRuntime {
      override fun inject(options: SubCircuitOptions): ClassName =
        if (options.useJavaxOnly) SubCircuitNames.INJECT_JAVAX else SubCircuitNames.INJECT

      override fun declarationInjects(options: SubCircuitOptions): Collection<ClassName> {
        // If explicitly using javax only look for javax, otherwise look for both.
        return if (options.useJavaxOnly) {
          listOf(SubCircuitNames.INJECT_JAVAX)
        } else {
          listOf(SubCircuitNames.INJECT, SubCircuitNames.INJECT_JAVAX)
        }
      }

      override val assisted: ClassName = SubCircuitNames.ASSISTED
      override val assistedInject: ClassName = SubCircuitNames.ASSISTED_INJECT
      override val assistedFactory: ClassName = SubCircuitNames.ASSISTED_FACTORY

      override fun asProvider(providedType: TypeName, options: SubCircuitOptions): TypeName {
        val className =
          if (options.useJavaxOnly) SubCircuitNames.PROVIDER_JAVAX else SubCircuitNames.PROVIDER
        return className.parameterizedBy(providedType)
      }

      override fun getProviderBlock(provider: CodeBlock): CodeBlock {
        return CodeBlock.of("%L.get()", provider)
      }
    }

    data object KotlinInject : InjectionRuntime {
      override fun inject(options: SubCircuitOptions) = SubCircuitNames.KotlinInject.INJECT

      override val assisted: ClassName = SubCircuitNames.KotlinInject.ASSISTED
      override val assistedInject: ClassName? = null
      override val assistedFactory: ClassName? = null // It has no annotation

      override fun asProvider(providedType: TypeName, options: SubCircuitOptions): TypeName {
        return LambdaTypeName.get(returnType = providedType)
      }

      override fun getProviderBlock(provider: CodeBlock): CodeBlock {
        return CodeBlock.of("%L()", provider)
      }
    }

    data object Metro : InjectionRuntime {
      override fun inject(options: SubCircuitOptions) = SubCircuitNames.Metro.INJECT

      override val assisted: ClassName = SubCircuitNames.Metro.ASSISTED
      override val assistedInject: ClassName? = null
      override val assistedFactory: ClassName = SubCircuitNames.Metro.ASSISTED_FACTORY

      override fun asProvider(providedType: TypeName, options: SubCircuitOptions): TypeName {
        return LambdaTypeName.get(returnType = providedType)
      }

      override fun getProviderBlock(provider: CodeBlock): CodeBlock {
        return CodeBlock.of("%L()", provider)
      }
    }
  }
}

internal class OriginAnnotation(val className: ClassName, val parameterName: String? = null)
