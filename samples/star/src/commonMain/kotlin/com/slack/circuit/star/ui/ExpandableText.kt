// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle

const val DEFAULT_MINIMUM_TEXT_LINE = 5

/**
 * A [Text] that can be expanded and collapsed.
 *
 * Adapted from https://stackoverflow.com/a/72982110
 */
@Suppress("LongParameterList")
@Composable
fun ExpandableText(
  text: String,
  modifier: Modifier = Modifier,
  textModifier: Modifier = Modifier,
  style: TextStyle = LocalTextStyle.current,
  fontStyle: FontStyle? = null,
  collapsedMaxLine: Int = DEFAULT_MINIMUM_TEXT_LINE,
  showMoreText: String = "... Show More",
  showMoreStyle: SpanStyle = SpanStyle(fontWeight = FontWeight.W500),
  showLessText: String = " Show Less",
  showLessStyle: SpanStyle = showMoreStyle,
  textAlign: TextAlign? = null,
  initiallyExpanded: Boolean = false,
) {
  var isExpanded by remember { mutableStateOf(initiallyExpanded) }
  var clickable by remember { mutableStateOf(false) }
  var lastCharIndex by remember { mutableIntStateOf(0) }
  Box(modifier = Modifier.clickable(clickable) { isExpanded = !isExpanded }.then(modifier)) {
    Text(
      modifier = textModifier.fillMaxWidth().animateContentSize(),
      text =
        buildAnnotatedString {
          if (clickable) {
            if (isExpanded) {
              append(text)
              withStyle(style = showLessStyle) { append(showLessText) }
            } else {
              val adjustText =
                text
                  .substring(startIndex = 0, endIndex = lastCharIndex)
                  .dropLast(showMoreText.length)
                  .dropLastWhile { it.isWhitespace() || it == '.' }
              append(adjustText)
              withStyle(style = showMoreStyle) { append(showMoreText) }
            }
          } else {
            append(text)
          }
        },
      maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLine,
      fontStyle = fontStyle,
      onTextLayout = { textLayoutResult ->
        if (!isExpanded && textLayoutResult.hasVisualOverflow) {
          clickable = true
          lastCharIndex = textLayoutResult.getLineEnd(collapsedMaxLine - 1)
        }
      },
      style = style,
      textAlign = textAlign,
    )
  }
}
