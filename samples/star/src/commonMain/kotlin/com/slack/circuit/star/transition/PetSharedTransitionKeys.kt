// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.transition

interface SharedTransitionKey

/** Transition key used for shared bounds transitions of a Pets screen. */
data class PetCardBoundsKey(val id: Long) : SharedTransitionKey

/** Transition key used for shared bounds transitions of a Pets name. */
data class PetNameBoundsKey(val id: Long) : SharedTransitionKey

/** Transition key used for shared bounds transitions of a Pets image container. */
data class PetImageBoundsKey(val id: Long, val index: Int) : SharedTransitionKey

/** Transition key used for shared element transitions of a Pets image. */
data class PetImageElementKey(val url: String) : SharedTransitionKey
