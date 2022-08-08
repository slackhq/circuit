package com.slack.circuit.sample.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.multibindings.Multibinds
import javax.inject.Provider

@Module
interface BaseUiModule {

  @Multibinds
  fun provideViewModelProviders(): Map<Class<out ViewModel>, ViewModel>
}