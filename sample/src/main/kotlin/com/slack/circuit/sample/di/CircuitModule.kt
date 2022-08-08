package com.slack.circuit.sample.di

import androidx.lifecycle.ViewModel
import com.slack.circuit.Navigator
import com.slack.circuit.NavigatorImpl
import com.slack.circuit.PresenterFactory
import com.slack.circuit.ScreenViewFactory
import com.slack.circuit.backstack.BackStackRecordLocalProviderViewModel
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.Multibinds

@Module
interface CircuitModule {
  @Multibinds
  fun presenterFactories(): Set<PresenterFactory>

  @Multibinds
  fun viewFactories(): Set<ScreenViewFactory>

  @Binds
  fun NavigatorImpl.Factory.bindNavigatorImpl(): Navigator.Factory<*>

  @ViewModelKey(BackStackRecordLocalProviderViewModel::class)
  @IntoMap
  @Binds
  fun BackStackRecordLocalProviderViewModel.bindBackStackRecordLocalProviderViewModel(): ViewModel
}