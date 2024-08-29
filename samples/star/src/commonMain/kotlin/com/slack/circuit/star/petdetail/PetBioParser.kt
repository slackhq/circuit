// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petdetail

import com.fleeksoft.ksoup.Ksoup

/** PetFinder doesn't expose pet bios so we have to do gross things :( */
internal object PetBioParser {
  private val newlines = Regex("\n{3,}")

  private fun String.reduceNewlines() = replace(newlines, "\n\n")

  internal fun parse(fullBody: String): String {
    return Ksoup.parse(fullBody, "https://www.petfinder.com/")
      .getElementsByClass("card-section")
      .first { node -> node.hasAttr("data-test") && node.attr("data-test") == "Pet_Story_Section" }
      .child(1)
      .wholeText()
      .trim()
      .reduceNewlines()
      .ifEmpty { error("List was empty! Usually this is a sign that parsing failed.") }
  }
}
