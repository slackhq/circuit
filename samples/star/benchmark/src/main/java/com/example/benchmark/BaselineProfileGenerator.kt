package com.example.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collectBaselineProfile(
        packageName = "com.slack.circuit.sample.star.apk",
        packageFilters = listOf(
            "com.slack.circuit"
        ),
        profileBlock = {
            startActivityAndWait()
        }
    )
}

