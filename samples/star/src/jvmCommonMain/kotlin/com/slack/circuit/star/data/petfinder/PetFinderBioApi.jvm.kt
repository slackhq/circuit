package com.slack.circuit.star.data.petfinder

import com.slack.circuit.star.petdetail.PetBioParser

actual suspend fun parsePetBio(html: String): String = PetBioParser.parse(html)
