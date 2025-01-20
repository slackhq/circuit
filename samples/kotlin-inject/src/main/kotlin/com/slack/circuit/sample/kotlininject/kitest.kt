// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.kotlininject

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.spi.FileSystemProvider
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Scope annotation class Singleton

@ContributesTo(AppScope::class)
interface MarkerInterface

@Singleton
@Component
abstract class ExampleComponent(@Component protected val fileSystemComponent: FileSystemComponent) {

  abstract fun example1(): Example1

  abstract fun example2(): Example2

  //  abstract fun <T> example3(): Example3<T>

  abstract fun example4(): Example4

  abstract fun example5(): Example5

  abstract fun example6(): Example6

  abstract fun example7(): Example7
}

@Component
interface FileSystemComponent {
  @Provides fun provideFileSystem(): FileSystem = FileSystems.getDefault()

  @Provides fun provideFileSystemProvider(fs: FileSystem): FileSystemProvider = fs.provider()
}

@Singleton class Example1 @Inject constructor(fs: FileSystem, optionalString: String = "hi")

@Singleton class Example2 @Inject constructor(fs: FileSystem)

// class Example3<T> @Inject constructor(fs: T)

class Example4 @Inject constructor()

class Example5 @Inject constructor()

class Example6 @Inject constructor(fs: Lazy<FileSystem>)

class Example7 @Inject constructor(fs: () -> FileSystem)

fun example() {
  val component = ExampleComponent::class.create(FileSystemComponent::class.create())
}
