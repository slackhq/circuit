// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.star.common.Strings
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.parcel.CommonParcelize
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@CommonParcelize
data object AboutScreen : Screen {
  object State : CircuitUiState
}

@CircuitInject(screen = AboutScreen::class, scope = AppScope::class)
@Composable
fun AboutPresenter(): AboutScreen.State = AboutScreen.State

@OptIn(ExperimentalResourceApi::class)
@CircuitInject(screen = AboutScreen::class, scope = AppScope::class)
@Composable
fun About(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(
      modifier = Modifier.size(96.dp),
      painter = painterResource("star_icon.png"),
      contentDescription = "STAR icon",
      tint = Color.Unspecified,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = Strings.ABOUT_SCREEN,
      textAlign = TextAlign.Justify,
      modifier = Modifier.widthIn(max = 400.dp).padding(horizontal = 16.dp),
    )
  }
}

// TODO this breaks kapt because it's only on desktop and not other platforms
// @Preview
// @Composable
// private fun AboutPreview() {
//  StarTheme { About() }
// }
