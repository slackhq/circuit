package com.slack.circuit.wizard.siblings

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.wizard.common.BackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildTopBar(title: String, modifier: Modifier = Modifier, onBack: () -> Unit) {
  CenterAlignedTopAppBar(
    title = { Text(title) },
    modifier = modifier,
    navigationIcon = { BackButton(onBack = onBack) }
  )
}