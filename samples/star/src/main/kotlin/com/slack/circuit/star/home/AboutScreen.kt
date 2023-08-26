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
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.star.R
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.ui.StarTheme
import kotlinx.parcelize.Parcelize

@Parcelize
data object AboutScreen : Screen {
  object State : CircuitUiState
}

@CircuitInject(screen = AboutScreen::class, scope = AppScope::class)
@Composable
fun AboutPresenter(): AboutScreen.State = AboutScreen.State

@CircuitInject(screen = AboutScreen::class, scope = AppScope::class)
@Composable
fun About(modifier: Modifier = Modifier) {
  Scaffold(
    modifier = modifier.fillMaxSize().padding(16.dp),
    content = { padding ->
      Column(
        modifier = Modifier.fillMaxSize().padding(padding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(
          modifier = Modifier.size(96.dp),
          painter = painterResource(R.drawable.star_icon),
          contentDescription = "STAR icon",
          tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(id = R.string.about_screen), textAlign = TextAlign.Justify)
      }
    }
  )
}

@Preview
@Composable
private fun AboutPreview() {
  StarTheme { About() }
}
