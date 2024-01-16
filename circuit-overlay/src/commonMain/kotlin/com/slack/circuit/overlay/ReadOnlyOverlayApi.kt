// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.overlay

/**
 * Indicates that the annotated API is a read-only of the Circuit Overlay API and not intended for
 * public implementation.
 */
@RequiresOptIn public annotation class ReadOnlyOverlayApi
