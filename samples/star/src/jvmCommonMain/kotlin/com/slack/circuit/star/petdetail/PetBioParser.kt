// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petdetail

import okhttp3.ResponseBody
import org.jsoup.Jsoup

/** PetFinder doesn't expose pet bios so we have to do gross things :( */
internal object PetBioParser {
  internal fun parse(body: ResponseBody): String {
    val fullBody = body.string()
    return Jsoup.parse(fullBody, "https://www.petfinder.com/")
      .getElementsByClass("card-section")
      .first { node -> node.hasAttr("data-test") && node.attr("data-test") == "Pet_Story_Section" }
      .child(1)
      .wholeText()
      .trim()
      .replace("\n\n\n", "\n\n")
      .ifEmpty { error("List was empty! Usually this is a sign that parsing failed.") }
  }
}
