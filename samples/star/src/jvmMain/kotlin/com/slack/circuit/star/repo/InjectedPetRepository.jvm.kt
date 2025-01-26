// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.repo

import com.slack.circuit.star.data.petfinder.PetBioParserApi
import com.slack.circuit.star.data.petfinder.PetfinderApi
import com.slack.circuit.star.db.SqlDriverFactory
import com.slack.circuit.star.di.AppScope
import dev.zacsweers.lattice.ContributesBinding
import dev.zacsweers.lattice.Inject
import dev.zacsweers.lattice.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class InjectedPetRepository(
  sqliteDriverFactory: SqlDriverFactory,
  private val petFinderApi: PetfinderApi,
  private val petBioParserApi: PetBioParserApi,
) : PetRepository by PetRepositoryImpl(sqliteDriverFactory, petFinderApi, petBioParserApi)
