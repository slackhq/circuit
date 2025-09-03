// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.slack.circuit.star.db.Gender.FEMALE
import com.slack.circuit.star.ui.StarTheme

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewUpdateFiltersSheet() {
  StarTheme {
    Surface {
      UpdateFiltersSheet(
        initialFilters = Filters(setOf(FEMALE)),
        modifier = Modifier.padding(16.dp),
      )
    }
  }
}
