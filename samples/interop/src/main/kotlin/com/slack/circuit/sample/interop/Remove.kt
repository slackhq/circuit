/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit.sample.interop

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

// Copied because it's from the icons-extended dependency, which is huge
@Suppress("MagicNumber")
val Remove: ImageVector
  get() {
    _remove?.let {
      return it
    }
    val remove =
      materialIcon(name = "Filled.Remove") {
        materialPath {
          moveTo(19.0f, 13.0f)
          horizontalLineTo(5.0f)
          verticalLineToRelative(-2.0f)
          horizontalLineToRelative(14.0f)
          verticalLineToRelative(2.0f)
          close()
        }
      }

    _remove = remove
    return remove
  }

private var _remove: ImageVector? = null
