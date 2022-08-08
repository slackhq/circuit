package com.slack.circuit.backstack

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import javax.inject.Inject

private fun Context.findActivity(): Activity? {
  var context = this
  while (context is ContextWrapper) {
    if (context is Activity) return context
    context = context.baseContext
  }
  return null
}

/** A [BackStackRecordLocalProvider] that provides [LocalViewModelStoreOwner] */
object ViewModelBackStackRecordLocalProvider : BackStackRecordLocalProvider<BackStack.Record> {
  @Composable
  override fun providedValuesFor(record: BackStack.Record): ProvidedValues {
    // Implementation note: providedValuesFor stays in the composition as long as the
    // back stack entry is present, which makes it safe for us to use composition
    // forget/abandon to clear the associated ViewModelStore if the host activity
    // isn't in the process of changing configurations.
    val containerViewModel = viewModel<BackStackRecordLocalProviderViewModel>()
    val viewModelStore = containerViewModel.viewModelStoreForKey(record.key)
    val activity = LocalContext.current.findActivity()
    remember(record, viewModelStore) {
      object : RememberObserver {
        override fun onAbandoned() {
          disposeIfNotChangingConfiguration()
        }

        override fun onForgotten() {
          disposeIfNotChangingConfiguration()
        }

        override fun onRemembered() {}

        fun disposeIfNotChangingConfiguration() {
          if (activity?.isChangingConfigurations != true) {
            containerViewModel.removeViewModelStoreOwnerForKey(record.key)?.clear()
          }
        }
      }
    }
    return remember(viewModelStore) {
      val list =
        listOf<ProvidedValue<*>>(
          LocalViewModelStoreOwner provides ViewModelStoreOwner { viewModelStore }
        )
      @Suppress("ObjectLiteralToLambda")
      object : ProvidedValues {
        @Composable override fun provideValues() = list
      }
    }
  }
}

// @ContributesMultibinding(AppScope::class)
// @ViewModelKey(BackStackRecordLocalProviderViewModel::class)
class BackStackRecordLocalProviderViewModel @Inject constructor() : ViewModel() {
  private val owners = mutableMapOf<String, ViewModelStore>()

  fun viewModelStoreForKey(key: String): ViewModelStore = owners.getOrPut(key) { ViewModelStore() }

  fun removeViewModelStoreOwnerForKey(key: String): ViewModelStore? = owners.remove(key)

  override fun onCleared() {
    owners.forEach { (_, store) -> store.clear() }
  }
}
