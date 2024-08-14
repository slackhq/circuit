// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.transition

/** Transition key used for shared bounds transitions of a Pets image container. */
data class PetImageBoundsSharedTransitionKey(val id: Long)

/** Transition key used for shared element transitions of a Pets image. */
data class PetImageSharedTransitionKey(val url: String)
