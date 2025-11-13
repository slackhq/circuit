package com.slack.circuit.backstack

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.runtime.screen.Screen
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RememberSaveableBackstackTest {

  @Test
  fun backStackStartsWithRootScreen() = runTest {
    moleculeFlow(RecompositionMode.Immediate) {
        val backStack = rememberSaveableBackStack(TestScreen.ScreenA)
        backStack.toList()
      }
      .test { assertEquals(awaitItem().first().screen, TestScreen.ScreenA) }
  }

  @Test
  fun rememberSaveableBackStackReturnsSameInstanceWithSameParams() = runTest {
    val rootScreen by mutableStateOf<Screen>(TestScreen.ScreenA)
    var dummyData by mutableStateOf(false)
    moleculeFlow(RecompositionMode.Immediate) {
        @Suppress("UNUSED_EXPRESSION") dummyData
        rememberSaveableBackStack(rootScreen)
      }
      .test {
        val firstStack = awaitItem()
        dummyData = true // Force a recomposition
        val secondStack = awaitItem()

        assertSame(firstStack, secondStack)
      }
  }

  @Test
  fun rememberSaveableBackStackReturnsNewInstanceWithChangedRoot() = runTest {
    var rootScreen by mutableStateOf<Screen>(TestScreen.ScreenA)
    moleculeFlow(RecompositionMode.Immediate) { rememberSaveableBackStack(rootScreen) }
      .test {
        val firstStack = awaitItem()
        rootScreen = TestScreen.ScreenB
        val secondStack = awaitItem()

        assertNotSame(firstStack, secondStack)
        assertEquals(firstStack.toList().first().screen, TestScreen.ScreenA)
        assertEquals(secondStack.toList().first().screen, TestScreen.ScreenB)
      }
  }

  @Test
  fun rememberSaveableBackStackStartedWithListReturnsNewInstanceWithChangedRoot() = runTest {
    var rootScreens by mutableStateOf<List<Screen>>(listOf(TestScreen.ScreenA))
    moleculeFlow(RecompositionMode.Immediate) { rememberSaveableBackStack(rootScreens) }
      .test {
        val firstStack = awaitItem()
        rootScreens = listOf(TestScreen.ScreenB)
        val secondStack = awaitItem()

        assertNotSame(firstStack, secondStack)
        assertEquals(firstStack.toList().first().screen, TestScreen.ScreenA)
        assertEquals(secondStack.toList().first().screen, TestScreen.ScreenB)
      }
  }
}
