// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import com.slack.circuit.star.di.AssistedInject
import dagger.BindsInstance
import dagger.Component
import dagger.Lazy
import dagger.MembersInjector
import dagger.Module
import dagger.Provides
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.multibindings.ElementsIntoSet
import dagger.multibindings.IntKey
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.spi.FileSystemProvider
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

interface SuperComponent {

}

@Singleton
@Component(modules = [FileSystemModule::class], dependencies = [CharSequenceComponent::class])
interface ExampleComponent {

  val stringValue: String

  val injector: MembersInjector<InjectableClass>
  val noInjector: MembersInjector<Example1>

  fun example1(): Lazy<Example1>

  fun example2(): Example2

  fun example4(): Example4

  fun example9Factory(): Example9.Factory<String>
  fun example9FactoryTwo(): Example9.Factory<Int>

  fun inject(injectableClass: Example1)

  fun inject(injectableClass: InjectableClass)

  @Component.Factory
  fun interface Factory {
    fun create(
      @BindsInstance stringValue: String,
      @BindsInstance @Named("named") stringValueNamed: String,
      component: CharSequenceComponent,
    ): ExampleComponent
  }
}

enum class CharSequenceComponent(val charSequence: CharSequence = "") {
  INSTANCE;

  fun interface Factory {
    fun create(@BindsInstance value: CharSequence): CharSequenceComponent
  }
}

@Module
interface FileSystemModule {
  companion object {
    @Provides @IntoSet fun provideString1(): String = "1"

    @Provides @ElementsIntoSet @Singleton fun provideString2(): Set<String> = setOf("2")

    @Provides @IntoMap @IntKey(1) fun provideMapInt1() = 1

    @Provides @IntoMap @Singleton @IntKey(2) fun provideMapInt2() = 2

    @Provides
    fun provideFileSystem(
      charSequenceValue: CharSequence,
      strings: Provider<Set<String>>,
      intMap: Map<Int, Int>,
      intProviderMap: @JvmSuppressWildcards Map<Int, Provider<Int>>,
    ): FileSystem = FileSystems.getDefault()

    @Singleton
    @Provides
    fun provideFileSystemProvider(fs: FileSystem): FileSystemProvider = fs.provider()
  }
}

abstract class Base {
  @set:Inject lateinit var fs: FileSystem
}
class InjectableClass @Inject constructor(stringValue: String) : Base() {
  @set:Inject lateinit var exampleComponent: ExampleComponent
  @Inject fun setterInject(fs: FileSystem, @Named("named") stringValue: String) {

  }
}

@Singleton class Example1 @Inject constructor(fs: FileSystem, fsProvider: FileSystemProvider)

class Example2 @Inject constructor(fs: FileSystem)

class Example3<T> @Inject constructor(fs: T)

class Example4 @Inject constructor()

class Example5<T> @Inject constructor()

class Example6<T> @Inject constructor(fs: dagger.Lazy<FileSystem>)

class Example7<T> @Inject constructor(fs: Provider<FileSystem>)

class Example8<T> @Inject constructor(fs: Provider<dagger.Lazy<FileSystem>>)

class Example9<T> @AssistedInject constructor(@Assisted intValue: Int, message: String) {
  @Singleton
  @AssistedFactory
  fun interface Factory<T> {
    fun create(intValue: Int): Example9<T>
  }
}
