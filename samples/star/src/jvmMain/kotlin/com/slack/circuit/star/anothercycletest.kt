package com.slack.circuit.star

import dagger.Binds
import dagger.Component
import dagger.Module
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import javax.inject.Inject
import javax.inject.Provider

class X @Inject constructor(val y: Y)

class Y @Inject constructor(
  val mapOfProvidersOfX: @JvmSuppressWildcards Map<String, Provider<X>>,
  // TODO is cycle detection not seeing through Map Providers?
  val mapOfProvidersOfY: @JvmSuppressWildcards Map<String, Provider<Y>>,
)

@Component(modules = [CycleModule::class])
interface CycleMultibindsGraph {
  fun y(): Y

}

@Module
interface CycleModule {
  @Binds
  @IntoMap
  @StringKey("X") fun x(value: X): X

  @Binds @IntoMap @StringKey("Y") fun y(value: Y): Y
}

//
//  DelegateFactory.setDelegate<Y>(
//    delegateFactory = <this>.#yProvider,
//    delegate = Y_Factory.create(
//      mapOfProvidersOfX = MapProviderFactory.builder<String, X>(size = 1)
//        .put(key = "X", providerOfValue = <this>.#xProvider)
//        .build(),
//      mapOfProvidersOfY = MapProviderFactory.builder<String, Y>(size = 1)
//        .put(key = "Y", providerOfValue = <this>.#yProvider)
//        .build()
//      )
//    )