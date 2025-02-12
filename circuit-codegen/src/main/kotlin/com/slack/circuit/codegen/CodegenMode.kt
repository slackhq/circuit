// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen

import com.google.devtools.ksp.processing.JvmPlatformInfo
import com.google.devtools.ksp.processing.PlatformInfo
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.slack.circuit.codegen.FactoryType.UI
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal enum class CodegenMode {
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

    override fun addInjectAnnotation(
      classBuilder: TypeSpec.Builder,
      constructorBuilder: FunSpec.Builder,
    ) {
      constructorBuilder.addAnnotation(runtime.inject)
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
          AnnotationSpec.builder(CircuitNames.DAGGER_MODULE).build(),
          AnnotationSpec.builder(CircuitNames.DAGGER_INSTALL_IN)
            .addMember("%T::class", scope)
            .build(),
          topLevelClass?.let {
            AnnotationSpec.builder(CircuitNames.DAGGER_ORIGINATING_ELEMENT)
              .addMember("%L = %T::class", "topLevelClass", topLevelClass)
              .build()
          },
        )

      val providerAnnotations =
        listOf(
          AnnotationSpec.builder(CircuitNames.DAGGER_BINDS).build(),
          AnnotationSpec.builder(CircuitNames.DAGGER_INTO_SET).build(),
        )

      val providerReturns =
        if (factoryType == UI) {
          CircuitNames.CIRCUIT_UI_FACTORY
        } else {
          CircuitNames.CIRCUIT_PRESENTER_FACTORY
        }

      val factoryName = factory.simpleName

      val providerSpec =
        FunSpec.builder("bind${factoryName}")
          .addModifiers(ABSTRACT)
          .addAnnotations(providerAnnotations)
          .addParameter(name = factoryName.replaceFirstChar { it.lowercase() }, type = factory)
          .returns(providerReturns)
          .build()

      return TypeSpec.classBuilder(factory.peerClass(factoryName + CircuitNames.MODULE))
        .addModifiers(ABSTRACT)
        .addAnnotations(moduleAnnotations)
        .addFunction(providerSpec)
        .build()
    }

    override fun addInjectAnnotation(
      classBuilder: TypeSpec.Builder,
      constructorBuilder: FunSpec.Builder,
    ) {
      constructorBuilder.addAnnotation(runtime.inject)
    }
  },

  /**
   * The `kotlin-inject` Anvil Codegen mode
   *
   * This mode annotates generated factory types with `ContributesMultibinding`, allowing for
   * KI-Anvil to automatically wire the generated class up to KI's multibinding system within a
   * given scope (e.g. AppScope).
   *
   * ```kotlin
   * @Inject
   * @ContributesMultibinding(AppScope::class)
   * public class FavoritesPresenterFactory(
   *   private val provider: () -> FavoritesPresenter,
   * ) : Presenter.Factory { ... }
   * ```
   */
  KOTLIN_INJECT_ANVIL {
    override val runtime: InjectionRuntime = InjectionRuntime.KotlinInject

    override fun supportsPlatforms(platforms: List<PlatformInfo>): Boolean {
      // KI-Anvil supports all
      return true
    }

    override fun annotateFactory(builder: TypeSpec.Builder, scope: TypeName) {
      builder.addAnnotation(
        AnnotationSpec.builder(CircuitNames.KotlinInject.Anvil.CONTRIBUTES_BINDING)
          .addMember("%T::class", scope)
          .addMember("multibinding = true")
          .build()
      )
    }

    override fun addInjectAnnotation(
      classBuilder: TypeSpec.Builder,
      constructorBuilder: FunSpec.Builder,
    ) {
      classBuilder.addAnnotation(runtime.inject)
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
   * This mode annotates generated factory types with `ContributesIntoSet`, allowing for
   * KI-Anvil to automatically wire the generated class up to KI's multibinding system within a
   * given scope (e.g. AppScope).
   *
   * ```kotlin
   * @Inject
   * @ContributesIntoSet(AppScope::class)
   * public class FavoritesPresenterFactory(
   *   private val provider: Provider<FavoritesPresenter>
   * ) : Presenter.Factory { ... }
   * ```
   */
  METRO {
    override val runtime: InjectionRuntime = InjectionRuntime.Metro

    override fun supportsPlatforms(platforms: List<PlatformInfo>): Boolean {
      // Metro supports all
      return true
    }

    override fun annotateFactory(builder: TypeSpec.Builder, scope: TypeName) {
      builder.addAnnotation(
        AnnotationSpec.builder(CircuitNames.Metro.CONTRIBUTES_INTO_SET)
          .addMember("%T::class", scope)
          .build()
      )
    }

    override fun addInjectAnnotation(
      classBuilder: TypeSpec.Builder,
      constructorBuilder: FunSpec.Builder,
    ) {
      classBuilder.addAnnotation(runtime.inject)
    }

    override fun filterValidInjectionSites(
      candidates: Collection<KSDeclaration>
    ): Collection<KSDeclaration> {
      return candidates.filter { it is KSFunctionDeclaration || it is KSClassDeclaration }
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

  abstract fun addInjectAnnotation(
    classBuilder: TypeSpec.Builder,
    constructorBuilder: FunSpec.Builder,
  )

  /** Filters the candidates for @Inject annotation placement. */
  open fun filterValidInjectionSites(
    candidates: Collection<KSDeclaration>
  ): Collection<KSDeclaration> = candidates.filterIsInstance<KSFunctionDeclaration>()

  open val runtime: InjectionRuntime = InjectionRuntime.Javax

  sealed interface InjectionRuntime {
    val inject: ClassName
    val assisted: ClassName

    fun asProvider(providedType: TypeName): TypeName

    fun getProviderBlock(provider: CodeBlock): CodeBlock

    data object Javax : InjectionRuntime {
      override val inject: ClassName = CircuitNames.INJECT
      override val assisted: ClassName = CircuitNames.ASSISTED

      override fun asProvider(providedType: TypeName): TypeName {
        return CircuitNames.PROVIDER.parameterizedBy(providedType)
      }

      override fun getProviderBlock(provider: CodeBlock): CodeBlock {
        return CodeBlock.of("%L.get()", provider)
      }
    }

    data object KotlinInject : InjectionRuntime {
      override val inject: ClassName = CircuitNames.KotlinInject.INJECT
      override val assisted: ClassName = CircuitNames.KotlinInject.ASSISTED

      override fun asProvider(providedType: TypeName): TypeName {
        return LambdaTypeName.get(returnType = providedType)
      }

      override fun getProviderBlock(provider: CodeBlock): CodeBlock {
        return CodeBlock.of("%L()", provider)
      }
    }

    data object Metro : InjectionRuntime {
      override val inject: ClassName = CircuitNames.Metro.INJECT
      override val assisted: ClassName = CircuitNames.Metro.ASSISTED

      override fun asProvider(providedType: TypeName): TypeName {
        return CircuitNames.Metro.PROVIDER.parameterizedBy(providedType)
      }

      override fun getProviderBlock(provider: CodeBlock): CodeBlock {
        return CodeBlock.of("%L()", provider)
      }
    }
  }
}
