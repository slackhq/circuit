/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit.sample.interop

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.slack.circuit.sample.counter.CounterEvent
import com.slack.circuit.sample.counter.CounterState
import com.slack.circuit.sample.interop.databinding.CounterViewBinding

/**
 * A traditional Android View implementation of a counter UI. It uses lightweight MVI semantics via
 * [setState] and setters for increment/decrement.
 */
class CounterView
@JvmOverloads
constructor(
  context: Context,
  attrs: AttributeSet? = null,
  @AttrRes defStyleAttr: Int = 0,
  @StyleRes defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

  private val binding: CounterViewBinding
  private var onIncrementClickListener: OnClickListener? = null
  private var onDecrementClickListener: OnClickListener? = null

  init {
    binding = CounterViewBinding.inflate(LayoutInflater.from(context), this, true)
    binding.increment.setOnClickListener { onIncrementClickListener?.onClick(it) }
    binding.decrement.setOnClickListener { onDecrementClickListener?.onClick(it) }
  }

  @SuppressLint("SetTextI18n")
  fun setState(count: Int) {
    binding.count.text = "Count: $count"
    if (count >= 0) {
      binding.count.setTextColor(Color.BLACK)
    } else {
      binding.count.setTextColor(Color.RED)
    }
  }

  fun setOnIncrementClickListener(listener: OnClickListener?) {
    onIncrementClickListener = listener
  }

  fun setOnDecrementClickListener(listener: OnClickListener?) {
    onDecrementClickListener = listener
  }
}

/** An interop compose function that renders [CounterView] as a Circuit-powered [AndroidView]. */
@Composable
fun CounterViewComposable(state: CounterState, modifier: Modifier = Modifier) {
  val eventSink = state.eventSink
  AndroidView(
    modifier = modifier.fillMaxSize(),
    factory = { context ->
      CounterView(context).apply {
        setOnIncrementClickListener { eventSink(CounterEvent.Increment) }
        setOnDecrementClickListener { eventSink(CounterEvent.Decrement) }
      }
    },
    update = { view -> view.setState(state.count) }
  )
}
