package com.slack.circuit.star.petlist

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.ui.StarTheme
import kotlinx.collections.immutable.persistentSetOf

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewUpdateFiltersSheet() {
  StarTheme {
    Surface {
      UpdateFiltersSheet(
        initialFilters = Filters(persistentSetOf(Gender.FEMALE)),
        modifier = Modifier.padding(16.dp),
      )
    }
  }
}