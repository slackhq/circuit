package com.slack.circuit.foundation.internal

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.slack.circuit.foundation.SharedElementTransitionLayout
import com.slack.circuit.foundation.SharedElementTransitionScope
import com.slack.circuit.runtime.InternalCircuitApi

/**
 * Implementation of [SharedTransitionScope] that does nothing. Used as a fallback for
 * [SharedElementTransitionScope] when a [SharedElementTransitionLayout] hasn't been setup.
 */
@InternalCircuitApi
@OptIn(ExperimentalSharedTransitionApi::class)
public data object NoOpSharedTransitionScope : SharedTransitionScope {

  override val isTransitionActive: Boolean = false

  override val Placeable.PlacementScope.lookaheadScopeCoordinates: LayoutCoordinates
    get() = this.coordinates!!

  override fun OverlayClip(clipShape: Shape): OverlayClip = ShapeBasedClip(clipShape)

  @Composable
  override fun rememberSharedContentState(key: Any): SharedContentState {
    @Suppress("invisible_reference", "invisible_member")
    return SharedContentState(key) // TODO Interface for this?
  }

  override fun Modifier.renderInSharedTransitionScopeOverlay(
    renderInOverlay: () -> Boolean,
    zIndexInOverlay: Float,
    clipInOverlayDuringTransition: (LayoutDirection, Density) -> Path?,
  ): Modifier = this

  override fun Modifier.sharedBounds(
    sharedContentState: SharedContentState,
    animatedVisibilityScope: AnimatedVisibilityScope,
    enter: EnterTransition,
    exit: ExitTransition,
    boundsTransform: BoundsTransform,
    resizeMode: SharedTransitionScope.ResizeMode,
    placeHolderSize: SharedTransitionScope.PlaceHolderSize,
    renderInOverlayDuringTransition: Boolean,
    zIndexInOverlay: Float,
    clipInOverlayDuringTransition: OverlayClip,
  ): Modifier = this

  override fun Modifier.sharedElement(
    state: SharedContentState,
    animatedVisibilityScope: AnimatedVisibilityScope,
    boundsTransform: BoundsTransform,
    placeHolderSize: SharedTransitionScope.PlaceHolderSize,
    renderInOverlayDuringTransition: Boolean,
    zIndexInOverlay: Float,
    clipInOverlayDuringTransition: OverlayClip,
  ): Modifier = this

  override fun Modifier.sharedElementWithCallerManagedVisibility(
    sharedContentState: SharedContentState,
    visible: Boolean,
    boundsTransform: BoundsTransform,
    placeHolderSize: SharedTransitionScope.PlaceHolderSize,
    renderInOverlayDuringTransition: Boolean,
    zIndexInOverlay: Float,
    clipInOverlayDuringTransition: OverlayClip,
  ): Modifier = this

  override fun Modifier.skipToLookaheadSize(): Modifier = this

  override fun LayoutCoordinates.toLookaheadCoordinates(): LayoutCoordinates = this

  private class ShapeBasedClip(val clipShape: Shape) : OverlayClip {
    private val path = Path()

    override fun getClipPath(
      state: SharedContentState,
      bounds: Rect,
      layoutDirection: LayoutDirection,
      density: Density,
    ): Path {
      path.reset()
      path.addOutline(clipShape.createOutline(bounds.size, layoutDirection, density))
      path.translate(bounds.topLeft)
      return path
    }
  }
}
