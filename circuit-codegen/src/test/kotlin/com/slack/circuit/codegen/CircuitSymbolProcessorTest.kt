package com.slack.circuit.codegen

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.CompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
class CircuitSymbolProcessorTest {
  private val appScope = kotlin(
    "AppScope.kt",
    """
      package test
      
      annotation class AppScope
    """.trimIndent()
  )
  private val screens = kotlin(
    "Screens.kt",
    """
      package test

      import com.slack.circuit.runtime.CircuitUiState 
      import com.slack.circuit.runtime.screen.Screen   

      object HomeScreen : Screen {
        data class State(val message: String) : CircuitUiState
      }
      data class FavoritesScreen(val userId: String) : Screen {
        data class State(val count: Int) : CircuitUiState
      }
    """.trimIndent()
  )

  @Test
  fun simpleUiFunction_withObjectScreen_noState() {
    assertGeneratedFile(
      sourceFile = kotlin(
        "TestUi.kt",
        """
          package test

          import com.slack.circuit.codegen.annotations.CircuitInject
          import androidx.compose.runtime.Composable
          import androidx.compose.ui.Modifier

          @CircuitInject(HomeScreen::class, AppScope::class)
          @Composable
          fun Home(modifier: Modifier = Modifier) {

          }
        """.trimIndent()
      ),
      generatedFilePath = "test/HomeFactory.kt",
      expectedContent = """
          package test
          
          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.CircuitUiState
          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.runtime.ui.Ui
          import com.slack.circuit.runtime.ui.ui
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject
          
          @ContributesMultibinding(AppScope::class)
          public class HomeFactory @Inject constructor() : Ui.Factory {
            override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
              HomeScreen -> ui<CircuitUiState> { _, modifier -> Home(modifier = modifier) }
              else -> null
            }
          }
        """.trimIndent()
    )
  }

  @Test
  fun simpleUiFunction_withClassScreen_noState() {
    assertGeneratedFile(
      sourceFile = kotlin(
        "TestUi.kt",
        """
          package test

          import com.slack.circuit.codegen.annotations.CircuitInject
          import androidx.compose.runtime.Composable
          import androidx.compose.ui.Modifier

          @CircuitInject(FavoritesScreen::class, AppScope::class)
          @Composable
          fun Favorites(modifier: Modifier = Modifier) {

          }
        """.trimIndent()
      ),
      generatedFilePath = "test/FavoritesFactory.kt",
      expectedContent = """
        package test
    
        import com.slack.circuit.runtime.CircuitContext
        import com.slack.circuit.runtime.CircuitUiState
        import com.slack.circuit.runtime.screen.Screen
        import com.slack.circuit.runtime.ui.Ui
        import com.slack.circuit.runtime.ui.ui
        import com.squareup.anvil.annotations.ContributesMultibinding
        import javax.inject.Inject
        
        @ContributesMultibinding(AppScope::class)
        public class FavoritesFactory @Inject constructor() : Ui.Factory {
          override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
            is FavoritesScreen -> ui<CircuitUiState> { _, modifier -> Favorites(modifier = modifier) }
            else -> null
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun simpleUiFunction_withInjectedClassScreen_noState() {
    assertGeneratedFile(
      sourceFile = kotlin(
        "TestUi.kt",
        """
          package test

          import com.slack.circuit.codegen.annotations.CircuitInject
          import androidx.compose.runtime.Composable
          import androidx.compose.ui.Modifier

          @CircuitInject(FavoritesScreen::class, AppScope::class)
          @Composable
          fun Favorites(screen: FavoritesScreen, modifier: Modifier = Modifier) {

          }
        """.trimIndent()
      ),
      generatedFilePath = "test/FavoritesFactory.kt",
      expectedContent = """
        package test
    
        import com.slack.circuit.runtime.CircuitContext
        import com.slack.circuit.runtime.CircuitUiState
        import com.slack.circuit.runtime.screen.Screen
        import com.slack.circuit.runtime.ui.Ui
        import com.slack.circuit.runtime.ui.ui
        import com.squareup.anvil.annotations.ContributesMultibinding
        import javax.inject.Inject
        
        @ContributesMultibinding(AppScope::class)
        public class FavoritesFactory @Inject constructor() : Ui.Factory {
          override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
            is FavoritesScreen -> ui<CircuitUiState> { _, modifier -> Favorites(modifier = modifier, screen
                = screen) }
            else -> null
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun simpleUiFunction_withObjectScreen_withState() {
    assertGeneratedFile(
      sourceFile = kotlin(
        "TestUi.kt",
        """
          package test

          import com.slack.circuit.codegen.annotations.CircuitInject
          import androidx.compose.runtime.Composable
          import androidx.compose.ui.Modifier

          @CircuitInject(HomeScreen::class, AppScope::class)
          @Composable
          fun Home(state: HomeScreen.State, modifier: Modifier = Modifier) {

          }
        """.trimIndent()
      ),
      generatedFilePath = "test/HomeFactory.kt",
      expectedContent = """
          package test
    
          import com.slack.circuit.runtime.CircuitContext
          import com.slack.circuit.runtime.screen.Screen
          import com.slack.circuit.runtime.ui.Ui
          import com.slack.circuit.runtime.ui.ui
          import com.squareup.anvil.annotations.ContributesMultibinding
          import javax.inject.Inject
          
          @ContributesMultibinding(AppScope::class)
          public class HomeFactory @Inject constructor() : Ui.Factory {
            override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
              HomeScreen -> ui<HomeScreen.State> { state, modifier -> Home(state = state, modifier = modifier) }
              else -> null
            }
          }
        """.trimIndent()
    )
  }

  @Test
  fun simpleUiFunction_withClassScreen_withState() {
    assertGeneratedFile(
      sourceFile = kotlin(
        "TestUi.kt",
        """
          package test

          import com.slack.circuit.codegen.annotations.CircuitInject
          import androidx.compose.runtime.Composable
          import androidx.compose.ui.Modifier

          @CircuitInject(FavoritesScreen::class, AppScope::class)
          @Composable
          fun Favorites(state: FavoritesScreen.State, modifier: Modifier = Modifier) {

          }
        """.trimIndent()
      ),
      generatedFilePath = "test/FavoritesFactory.kt",
      expectedContent = """
        package test
    
        import com.slack.circuit.runtime.CircuitContext
        import com.slack.circuit.runtime.screen.Screen
        import com.slack.circuit.runtime.ui.Ui
        import com.slack.circuit.runtime.ui.ui
        import com.squareup.anvil.annotations.ContributesMultibinding
        import javax.inject.Inject
        
        @ContributesMultibinding(AppScope::class)
        public class FavoritesFactory @Inject constructor() : Ui.Factory {
          override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
            is FavoritesScreen -> ui<FavoritesScreen.State> { state, modifier -> Favorites(state = state, modifier = modifier) }
            else -> null
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun simpleUiFunction_withInjectedClassScreen_withState() {
    assertGeneratedFile(
      sourceFile = kotlin(
        "TestUi.kt",
        """
          package test

          import com.slack.circuit.codegen.annotations.CircuitInject
          import androidx.compose.runtime.Composable
          import androidx.compose.ui.Modifier

          @CircuitInject(FavoritesScreen::class, AppScope::class)
          @Composable
          fun Favorites(state: FavoritesScreen.State, screen: FavoritesScreen, modifier: Modifier = Modifier) {

          }
        """.trimIndent()
      ),
      generatedFilePath = "test/FavoritesFactory.kt",
      expectedContent = """
        package test
    
        import com.slack.circuit.runtime.CircuitContext
        import com.slack.circuit.runtime.screen.Screen
        import com.slack.circuit.runtime.ui.Ui
        import com.slack.circuit.runtime.ui.ui
        import com.squareup.anvil.annotations.ContributesMultibinding
        import javax.inject.Inject
        
        @ContributesMultibinding(AppScope::class)
        public class FavoritesFactory @Inject constructor() : Ui.Factory {
          override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
            is FavoritesScreen -> ui<FavoritesScreen.State> { state, modifier -> Favorites(state = state, modifier = modifier, screen
                = screen) }
            else -> null
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun uiClass_noInjection() {
    assertGeneratedFile(
      sourceFile = kotlin(
        "TestUi.kt",
        """
          package test

          import com.slack.circuit.codegen.annotations.CircuitInject
          import com.slack.circuit.runtime.ui.Ui
          import androidx.compose.runtime.Composable
          import androidx.compose.ui.Modifier

          @CircuitInject(FavoritesScreen::class, AppScope::class)
          @Composable
          class Favorites : Ui<FavoritesScreen.State> {
            @Composable
            override fun Content(state: FavoritesScreen.State, modifier: Modifier) {

            }
          }
        """.trimIndent()
      ),
      generatedFilePath = "test/FavoritesFactory.kt",
      expectedContent = """
        package test
    
        import com.slack.circuit.runtime.CircuitContext
        import com.slack.circuit.runtime.screen.Screen
        import com.slack.circuit.runtime.ui.Ui
        import com.squareup.anvil.annotations.ContributesMultibinding
        import javax.inject.Inject
        
        @ContributesMultibinding(AppScope::class)
        public class FavoritesFactory @Inject constructor() : Ui.Factory {
          override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
            is FavoritesScreen -> Favorites()
            else -> null
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun uiClass_simpleInjection() {
    assertGeneratedFile(
      sourceFile = kotlin(
        "TestUi.kt",
        """
          package test

          import com.slack.circuit.codegen.annotations.CircuitInject
          import com.slack.circuit.runtime.ui.Ui
          import androidx.compose.runtime.Composable
          import androidx.compose.ui.Modifier
          import javax.inject.Inject

          @CircuitInject(FavoritesScreen::class, AppScope::class)
          @Composable
          class Favorites @Inject constructor() : Ui<FavoritesScreen.State> {
            @Composable
            override fun Content(state: FavoritesScreen.State, modifier: Modifier) {

            }
          }
        """.trimIndent()
      ),
      generatedFilePath = "test/FavoritesFactory.kt",
      expectedContent = """
        package test
    
        import com.slack.circuit.runtime.CircuitContext
        import com.slack.circuit.runtime.screen.Screen
        import com.slack.circuit.runtime.ui.Ui
        import com.squareup.anvil.annotations.ContributesMultibinding
        import javax.inject.Inject
        import javax.inject.Provider
        
        @ContributesMultibinding(AppScope::class)
        public class FavoritesFactory @Inject constructor(
          private val provider: Provider<Favorites>,
        ) : Ui.Factory {
          override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
            is FavoritesScreen -> provider.get()
            else -> null
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun uiClass_assistedInjection() {
    assertGeneratedFile(
      sourceFile = kotlin(
        "TestUi.kt",
        """
          package test

          import com.slack.circuit.codegen.annotations.CircuitInject
          import com.slack.circuit.runtime.ui.Ui
          import androidx.compose.runtime.Composable
          import androidx.compose.ui.Modifier
          import dagger.assisted.Assisted
          import dagger.assisted.AssistedFactory
          import dagger.assisted.AssistedInject

          @Composable
          class Favorites @AssistedInject constructor(
            @Assisted private val screen: FavoritesScreen,
          ) : Ui<FavoritesScreen.State> {
            @CircuitInject(FavoritesScreen::class, AppScope::class)
            @AssistedFactory
            fun interface Factory {
              fun create(screen: FavoritesScreen): Favorites
            }
          
            @Composable
            override fun Content(state: FavoritesScreen.State, modifier: Modifier) {

            }
          }
        """.trimIndent()
      ),
      generatedFilePath = "test/FavoritesFactory.kt",
      expectedContent = """
        package test
    
        import com.slack.circuit.runtime.CircuitContext
        import com.slack.circuit.runtime.screen.Screen
        import com.slack.circuit.runtime.ui.Ui
        import com.squareup.anvil.annotations.ContributesMultibinding
        import javax.inject.Inject
        
        @ContributesMultibinding(AppScope::class)
        public class FavoritesFactory @Inject constructor(
          private val factory: Favorites.Factory,
        ) : Ui.Factory {
          override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
            is FavoritesScreen -> factory.create(screen = screen)
            else -> null
          }
        }
      """.trimIndent()
    )
  }

  // TODO
  //  - presenter functions
  //  - presenter classes
  //  - presenter classes/functions with injections (screen and navigator)
  //  - presenter functions without return values
  //  - injecting invalid injections

  private fun assertGeneratedFile(
    sourceFile: SourceFile,
    generatedFilePath: String,
    @Language("kotlin") expectedContent: String
  ) {
    val compilation = prepareCompilation(sourceFile)
    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(ExitCode.OK)
    val generatedSourcesDir = compilation.kspSourcesDir
    val generatedAdapter = File(generatedSourcesDir, "kotlin/$generatedFilePath")
    assertThat(generatedAdapter.exists()).isTrue()
    assertThat(generatedAdapter.readText().trim())
      .isEqualTo(expectedContent.trimIndent())
  }

  private fun prepareCompilation(
    vararg sourceFiles: SourceFile,
  ): KotlinCompilation =
    KotlinCompilation().apply {
      sources = sourceFiles.toList() + listOf(appScope, screens)
      inheritClassPath = true
      symbolProcessorProviders = listOf(CircuitSymbolProcessorProvider())
    }

  private fun compile(
    vararg sourceFiles: SourceFile,
  ): CompilationResult {
    return prepareCompilation(*sourceFiles).compile()
  }
}