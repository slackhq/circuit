// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.property

/** Extension for configuring Circuit project conventions. */
abstract class CircuitProjectExtension @Inject constructor(objects: ObjectFactory, providers: ProviderFactory) {
  /** Whether Compose is enabled for this project. Defaults to true. */
  val hasCompose: Property<Boolean> = objects.property<Boolean>().convention(true)
}
