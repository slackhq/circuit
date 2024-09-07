package com.slack.circuit.sample.kotlininject

import kotlin.reflect.KClass
import me.tatarka.inject.annotations.Scope

abstract class AppScope private constructor()

@Scope
annotation class SingleIn(val scope: KClass<*>)