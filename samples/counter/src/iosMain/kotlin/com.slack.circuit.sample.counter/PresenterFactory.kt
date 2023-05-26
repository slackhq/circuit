package com.slack.circuit.sample.counter

import com.slack.circuit.swiftui.swiftUIPresenterOf

object PresenterFactory {
    fun counterPresenter() = swiftUIPresenterOf { CounterPresenter(it) }
    fun primePresenter(number: Int) = swiftUIPresenterOf { PrimePresenter(it, number) }
}
