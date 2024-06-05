// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data.petfinder

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class AnimalsResponse(val animals: List<Animal>, val pagination: Pagination)

@Serializable
data class Animal(
  val id: Long,
  @SerialName("organization_id") val organizationId: String,
  val url: String,
  val type: String,
  val species: String,
  val gender: String,
  val breeds: Breeds,
  val colors: Colors,
  val age: String,
  val size: String,
  val coat: String? = null,
  val name: String,
  val description: String? = null,
  val photos: List<Photo>,
  val videos: List<Video>,
  val status: String,
  // "attributes": {
  //     "spayed_neutered": true,
  //     "house_trained": true,
  //     "declawed": false,
  //     "special_needs": false,
  //     "shots_current": true
  // },
  val attributes: Map<String, Boolean?>,
  // "environment": {
  //     "children": false,
  //     "dogs": true,
  //     "cats": true
  // },
  val environment: Map<String, Boolean?>,
  val tags: List<String>,
  // "contact": {
  //     "email": "petfindertechsupport@gmail.com",
  //     "phone": "555-555-5555",
  //     "address": {
  //         "address1": null,
  //         "address2": null,
  //         "city": "Jersey City",
  //         "state": "NJ",
  //         "postcode": "07097",
  //         "country": "US"
  //     }
  // },
  //  val contact: Contact,
  // "2018-09-04T14:49:09+0000"
  @SerialName("published_at") val publishedAt: String,
  // 0.4095
  val distance: Double? = null, // TODO wtf are the units of this
  // "_links": {
  //     "self": {
  //         "href": "/v2/animals/124"
  //     },
  //     "type": {
  //         "href": "/v2/types/cat"
  //     },
  //     "organization": {
  //         "href": "/v2/organizations/nj333"
  //     }
  // }
  @SerialName("_links") val links: Links,
)

@Serializable data class Links(val self: Link, val type: Link, val organization: Link)

@Serializable data class Link(val href: String)

@Serializable
data class Breeds(
  val primary: String? = null,
  val secondary: String? = null,
  val mixed: Boolean = false,
  val unknown: Boolean = false,
)

@Serializable
data class Colors(
  val primary: String? = null,
  val secondary: String? = null,
  val tertiary: String? = null,
)

@Serializable
data class Photo(val small: String, val medium: String, val large: String, val full: String)

@Serializable
data class Video(
  // An iframe snippet to embed
  val embed: String
)

@Serializable
data class Pagination(
  @SerialName("count_per_page") val countPerPage: Int = 20,
  @SerialName("total_count") val totalCount: Int = 320,
  @SerialName("current_page") val currentPage: Int = 1,
  @SerialName("total_pages") val totalPages: Int = 16,
)
