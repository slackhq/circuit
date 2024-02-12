package com.slack.circuit.codegen

import com.google.devtools.ksp.processing.JvmPlatformInfo
import com.google.devtools.ksp.processing.PlatformInfo
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
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
    override fun supportsPlatforms(platforms: List<PlatformInfo>): Boolean {
      // Anvil only supports JVM & Android
      return platforms.all { it is JvmPlatformInfo }
    }

    override fun annotateFactory(builder: TypeSpec.Builder, scope: TypeName) {
      builder.addAnnotation(
        AnnotationSpec.builder(ContributesMultibinding::class).addMember("%T::class", scope).build()
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
          AnnotationSpec.builder(CircuitNames.DAGGER_MODULE).build(),
          AnnotationSpec.builder(CircuitNames.DAGGER_INSTALL_IN).addMember("%T::class", scope).build(),
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
        if (factoryType == FactoryType.UI) {
          CircuitNames.CIRCUIT_UI_FACTORY
        } else {
          CircuitNames.CIRCUIT_PRESENTER_FACTORY
        }

      val factoryName = factory.simpleName

      val providerSpec =
        FunSpec.builder("bind${factoryName}")
          .addModifiers(KModifier.ABSTRACT)
          .addAnnotations(providerAnnotations)
          .addParameter(name = factoryName.replaceFirstChar { it.lowercase() }, type = factory)
          .returns(providerReturns)
          .build()

      return TypeSpec.classBuilder(factory.peerClass(factoryName + CircuitNames.MODULE))
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