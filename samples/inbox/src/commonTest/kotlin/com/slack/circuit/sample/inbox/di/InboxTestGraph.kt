// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.di

import com.slack.circuit.sample.inbox.data.EmailRepository
import com.slack.circuit.sample.inbox.detail.EmailDetailPresenter
import com.slack.circuit.sample.inbox.list.InboxListPresenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory

/**
 * Test graph that exposes the same assisted presenter factories used by production.
 *
 * The factory accepts a provided [EmailRepository] so tests can seed data without replacing the
 * generated child factories.
 */
@DependencyGraph(scope = AppScope::class)
interface InboxTestGraph {
  val inboxListFactory: InboxListPresenter.Factory
  val emailDetailFactory: EmailDetailPresenter.Factory

  @DependencyGraph.Factory
  fun interface Factory {
    fun create(@Provides emailRepository: EmailRepository): InboxTestGraph
  }
}

internal fun inboxTestGraph(repository: EmailRepository): InboxTestGraph =
  createGraphFactory<InboxTestGraph.Factory>().create(repository)
