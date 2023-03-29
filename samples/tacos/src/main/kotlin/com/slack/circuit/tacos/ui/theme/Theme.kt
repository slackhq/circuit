// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.graphics.Color

private val LightMaterialColors =
  lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
  )

private val DarkMaterialColors =
  darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
  )

private val LightTacoColors =
  TacoColorScheme(
    primaryBottomBarContainer = tc_light_bottomBarRoseContainer,
    secondaryBottomBarContainer = tc_light_bottomBarRedContainer,
    onPrimaryBottomBarContainer = tc_light_onBottomBarRoseContainer,
    onSecondaryBottomBarContainer = tc_light_onBottomBarRedContainer,
  )

private val DarkTacoColors =
  TacoColorScheme(
    primaryBottomBarContainer = tc_dark_bottomBarRoseContainer,
    secondaryBottomBarContainer = tc_dark_bottomBarRedContainer,
    onPrimaryBottomBarContainer = tc_dark_onBottomBarRoseContainer,
    onSecondaryBottomBarContainer = tc_dark_onBottomBarRedContainer,
  )

@Composable
fun TacoTheme(useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  val materialColors =
    when {
      useDarkTheme -> DarkMaterialColors
      else -> LightMaterialColors
    }
  val tacoColors =
    when {
      useDarkTheme -> DarkTacoColors
      else -> LightTacoColors
    }

  MaterialTheme(colorScheme = materialColors) {
    CompositionLocalProvider(LocalTacoColorScheme provides tacoColors, content = content)
  }
}

private val LocalTacoColorScheme = staticCompositionLocalOf { LightTacoColors }

object TacoTheme {
  val colorScheme: TacoColorScheme
    @Composable @ReadOnlyComposable get() = LocalTacoColorScheme.current
}

/** A color scheme holds all named color parameters for a [TacoTheme]. */
@Stable
class TacoColorScheme(
  primaryBottomBarContainer: Color,
  secondaryBottomBarContainer: Color,
  onPrimaryBottomBarContainer: Color,
  onSecondaryBottomBarContainer: Color,
) {
  var primaryBottomBarContainer by
    mutableStateOf(primaryBottomBarContainer, structuralEqualityPolicy())
    internal set
  var secondaryBottomBarContainer by
    mutableStateOf(secondaryBottomBarContainer, structuralEqualityPolicy())
    internal set
  var onPrimaryBottomBarContainer by
    mutableStateOf(onPrimaryBottomBarContainer, structuralEqualityPolicy())
    internal set
  var onSecondaryBottomBarContainer by
    mutableStateOf(onSecondaryBottomBarContainer, structuralEqualityPolicy())
    internal set
}
