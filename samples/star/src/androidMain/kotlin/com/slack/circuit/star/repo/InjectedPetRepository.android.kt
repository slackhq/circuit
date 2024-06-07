// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.repo

import com.slack.circuit.star.data.petfinder.PetfinderApi
import com.slack.circuit.star.db.SqlDriverFactory
import com.slack.circuit.star.di.AppScope
import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.optional.SingleIn
import javax.inject.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class InjectedPetRepository
@Inject
constructor(sqliteDriverFactory: SqlDriverFactory, private val petFinderApi: PetfinderApi) :
  PetRepository by PetRepositoryImpl(sqliteDriverFactory, petFinderApi)
