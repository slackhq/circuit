// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.datastore

import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import okio.FileSystem
import okio.Path

private const val PREFERENCES_EXTENSION = "preferences_pb"

val Path.extension: String
  get() = name.substringAfterLast('.', "")

fun createStorage(fs: FileSystem, producePath: FileSystem.() -> Path): Storage<Preferences> {
  return OkioStorage(fs, PreferencesSerializer) {
    val path = fs.producePath()
    check(path.extension == PREFERENCES_EXTENSION) {
      "File extension for file: $path does not match required extension for" +
        " Preferences file: $PREFERENCES_EXTENSION"
    }
    path
  }
}
