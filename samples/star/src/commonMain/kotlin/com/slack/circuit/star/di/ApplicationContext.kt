// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import dev.zacsweers.metro.Qualifier

/**
 * Qualifier to denote a `Context` or `PlatformContext` that is specifically an Application context.
 */
@Qualifier annotation class ApplicationContext
