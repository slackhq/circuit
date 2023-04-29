package com.slack.circuit.sample.counter

import androidx.compose.runtime.LaunchedEffect
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.sample.counter.util.IO
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/** A basic [Flow]-based presenter interface. */
fun interface FlowPresenter<UiState : Any, UiEvent : Any> {
  fun present(scope: CoroutineScope, events: Flow<UiEvent>): StateFlow<UiState>
}

/**
 * Interop from a Circuit [Presenter] to a [FlowPresenter].
 *
 * Nuance here is that this needs to know how to access the underlying event sink.
 */
// Used in Swift
// TODO let's try to generify this pattern somehow.
class FlowCounterPresenter : FlowPresenter<Int, CounterScreen.Event> {
  private val circuitPresenter = presenterOf { CounterPresenter(Navigator.NoOp) }

  override fun present(
    scope: CoroutineScope,
    events: Flow<CounterScreen.Event>
  ): StateFlow<Int> {
    return scope
      .launchMolecule(RecompositionClock.Immediate) {
        val (count, eventSink) = circuitPresenter.present()
        LaunchedEffect(eventSink) { events.collect(eventSink) }
        count
      }
  }

  fun createSwiftCounterScreen(): SwiftCounterScreen<Int, CounterScreen.Event> {
    val channel = Channel<CounterScreen.Event>(Channel.BUFFERED)
    val eventsFlow = channel.receiveAsFlow()
    return SwiftCounterScreen({ scope -> present(scope, eventsFlow) }, channel::trySend)
  }
}

// Adapted from the KotlinConf app
// https://github.com/JetBrains/kotlinconf-app/blob/642404f3454d384be966c34d6b254b195e8d2892/shared/src/commonMain/kotlin/org/jetbrains/kotlinconf/utils/Coroutines.kt#L6
// TODO let's try to generify this pattern somehow.
class SwiftCounterScreen<T, E>(
  private val stateFlow: (CoroutineScope) -> StateFlow<T>,
  private val eventSink: (E) -> Unit
) : CoroutineScope {
  private val exceptionHandler: CoroutineExceptionHandler = object : CoroutineExceptionHandler {
    override val key: CoroutineContext.Key<*> = CoroutineExceptionHandler

    override fun handleException(context: CoroutineContext, exception: Throwable) {
      println("Failed with exception: ${exception.message}")
      exception.printStackTrace()
    }
  }

  override val coroutineContext: CoroutineContext =
    SupervisorJob() + exceptionHandler + Dispatchers.IO

  // TODO can we use the scope of the enclosing class instead?
  @OptIn(DelicateCoroutinesApi::class)
  fun subscribe(block: (T) -> Unit) =
    GlobalScope.launch(Dispatchers.IO) { stateFlow(this).collect { block(it) } }

  // TODO can we use the scope of the enclosing class instead?
  @OptIn(DelicateCoroutinesApi::class)
  fun send(event: E) = GlobalScope.launch(Dispatchers.IO) { eventSink(event) }
}
