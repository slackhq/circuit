// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.Preferences
import com.slack.circuit.star.di.AppScope
import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.optional.SingleIn
import javax.inject.Inject

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class InjectedTokenStorage @Inject constructor(storage: Storage<Preferences>) :
  TokenStorage by TokenStorageImpl(storage)
