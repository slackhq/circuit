// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.test

@Suppress("ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE", "INCOMPATIBLE_MATCHING")
public actual sealed interface BaseTestEventSinkType<UiEvent> : (UiEvent) -> Unit
