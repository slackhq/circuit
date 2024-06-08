// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data.petfinder

import com.slack.circuit.star.petdetail.PetBioParser

actual suspend fun parsePetBio(html: String): String = PetBioParser.parse(html)
