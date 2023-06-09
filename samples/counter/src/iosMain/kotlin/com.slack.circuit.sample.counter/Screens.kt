package com.slack.circuit.sample.counter

import com.slack.circuit.swiftui.SwiftUIPresenter
import com.slack.circuit.swiftui.swiftUIPresenterOf

object IosCounterScreen: CounterScreen, SwiftUIPresenter.Factory<CounterScreen.State> {
    override fun presenter() = swiftUIPresenterOf { CounterPresenter(it) }
}

data class IosPrimeScreen(
    override val number: Int
): PrimeScreen, SwiftUIPresenter.Factory<PrimeScreen.State> {
    override fun presenter() = swiftUIPresenterOf { PrimePresenter(it, number) }
}
