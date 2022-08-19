package com.slack.circuit.sample.petlist

import androidx.compose.runtime.Composable
import app.cash.molecule.RecompositionClock
import app.cash.molecule.RecompositionClock.Immediate
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Foo {
    @Test
    fun bar() = runTest {
        @Suppress("DEPRECATION")
        moleculeFlow(Immediate) {
            Test("baz")
        }.test {
            assertEquals("baz", awaitItem())
        }
    }

//    @Test
//    fun bar() {
//        @Suppress("DEPRECATION")
//        testMolecule(
//            body = { Test("baz") }
//        ) {
//            assertEquals("baz", awaitItem())
//        }
//    }

    @Composable
    private fun Test(word: String): String {
        return word
    }
}