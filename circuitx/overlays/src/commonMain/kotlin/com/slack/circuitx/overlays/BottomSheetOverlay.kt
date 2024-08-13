// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.overlays

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.slack.circuit.foundation.internal.BackHandler
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayNavigator
import com.slack.circuit.runtime.internal.rememberStableCoroutineScope
import kotlinx.coroutines.launch

/**
 * An [Overlay] that shows a [ModalBottomSheet] with the given [model] and [content]. When the sheet
 * is dismissed, it emits a [Result] type to the [OverlayNavigator]. This is useful for showing
 * temporary content in a sheet that you want to return back to the calling UI, such as a picker UI
 * or user input prompt.
 *
 * @property model The model to pass to the [content] composable. This should be thought of like a
 *   state model.
 * @property dismissOnTapOutside Controls whether the sheet can be dismissed by tapping outside of
 *   it.
 * @property onDismiss A callback that is invoked when the sheet is dismissed with a result. This is
 *   required if [dismissOnTapOutside] is true.
 * @property content The Composable content to show in the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
public class BottomSheetOverlay<Model : Any, Result : Any>
private constructor(
  private val model: Model,
  private val dismissOnTapOutside: Boolean,
  private val onDismiss: (() -> Result)?,
  private val sheetShape: Shape?,
  private val sheetContainerColor: Color?,
  private val dragHandle: (@Composable () -> Unit)?,
  private val skipPartiallyExpandedState: Boolean,
  private val properties: ModalBottomSheetProperties,
  private val content: @Composable (Model, OverlayNavigator<Result>) -> Unit,
) : Overlay<Result> {

  /**
   * Constructs a new [BottomSheetOverlay] that will not dismiss when tapped outside of the sheet.
   * This means that only the [content] can finish the overlay. Additionally the appearance of the
   * sheet can be customized.
   *
   * @param sheetContainerColor set the container color of the ModalBottomSheet
   * @param dragHandle customize the drag handle of the sheet
   * @param skipPartiallyExpandedState indicates if the Sheet should be expanded per default (if
   *   it's height exceed the partial height threshold)
   * @param isFocusable corresponds to [ModalBottomSheetProperties.isFocusable] and will be passed
   *   on to the final sheet as such.
   */
  public constructor(
    model: Model,
    sheetContainerColor: Color? = null,
    sheetShape: Shape? = null,
    dragHandle: @Composable (() -> Unit)? = null,
    skipPartiallyExpandedState: Boolean = false,
    isFocusable: Boolean = true,
    content: @Composable (Model, OverlayNavigator<Result>) -> Unit,
  ) : this(
    model = model,
    dismissOnTapOutside = false,
    onDismiss = null,
    dragHandle = dragHandle,
    sheetShape = sheetShape,
    sheetContainerColor = sheetContainerColor,
    skipPartiallyExpandedState = skipPartiallyExpandedState,
    properties =
      ModalBottomSheetProperties(isFocusable = isFocusable, shouldDismissOnBackPress = false),
    content = content,
  )

  /**
   * Constructs a new [BottomSheetOverlay] that will dismiss when tapped outside of the sheet.
   * [onDismiss] is required in this case to offer a default value in this event. Additionally the
   * appearance of the sheet can be customized.
   *
   * @param sheetContainerColor set the container color of the ModalBottomSheet
   * @param dragHandle customize the drag handle of the sheet
   * @param skipPartiallyExpandedState indicates if the Sheet should be expanded per default (if
   *   it's height exceed the partial height threshold)
   * @param properties any [ModalBottomSheetProperties]. Defaults to
   *   [ModalBottomSheetDefaults.properties].
   */
  public constructor(
    model: Model,
    onDismiss: (() -> Result),
    sheetContainerColor: Color? = null,
    sheetShape: Shape? = null,
    dragHandle: @Composable (() -> Unit)? = null,
    skipPartiallyExpandedState: Boolean = false,
    properties: ModalBottomSheetProperties = ModalBottomSheetDefaults.properties(),
    content: @Composable (Model, OverlayNavigator<Result>) -> Unit,
  ) : this(
    model = model,
    dismissOnTapOutside = true,
    onDismiss = onDismiss,
    dragHandle = dragHandle,
    sheetShape = sheetShape,
    sheetContainerColor = sheetContainerColor,
    skipPartiallyExpandedState = skipPartiallyExpandedState,
    properties = properties,
    content = content,
  )

  @Composable
  override fun Content(navigator: OverlayNavigator<Result>) {
    var hasShown by remember { mutableStateOf(false) }
    val sheetState =
      rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpandedState,
        confirmValueChange = { newValue ->
          if (hasShown && newValue == SheetValue.Hidden) {
            dismissOnTapOutside
          } else {
            true
          }
        },
      )

    var pendingResult by remember { mutableStateOf<Result?>(null) }
    ModalBottomSheet(
      content = {
        val coroutineScope = rememberStableCoroutineScope()
        BackHandler(enabled = sheetState.isVisible) {
          coroutineScope
            .launch { sheetState.hide() }
            .invokeOnCompletion {
              if (!sheetState.isVisible) {
                navigator.finish(onDismiss!!.invoke())
              }
            }
        }
        // Delay setting the result until we've finished dismissing
        content(model) { result ->
          // This is the OverlayNavigator.finish() callback
          coroutineScope.launch {
            pendingResult = result
            sheetState.hide()
          }
        }
      },
      sheetState = sheetState,
      shape = sheetShape ?: RoundedCornerShape(32.dp),
      containerColor = sheetContainerColor ?: BottomSheetDefaults.ContainerColor,
      dragHandle = dragHandle ?: { BottomSheetDefaults.DragHandle() },
      // Go edge-to-edge
      windowInsets = WindowInsets(0, 0, 0, 0),
      onDismissRequest = {
        // Only possible if dismissOnTapOutside is false
        check(dismissOnTapOutside)
        navigator.finish(onDismiss!!.invoke())
      },
      properties = properties,
    )

    LaunchedEffect(model, onDismiss) {
      snapshotFlow { sheetState.currentValue }
        .collect { newValue ->
          if (hasShown && newValue == SheetValue.Hidden) {
            // This is apparently as close as we can get to an "onDismiss" callback, which
            // unfortunately has no animation
            val result = pendingResult ?: onDismiss?.invoke() ?: error("no result!")
            navigator.finish(result)
          }
        }
    }
    LaunchedEffect(model, onDismiss) {
      hasShown = true
      sheetState.show()
    }
  }
}
