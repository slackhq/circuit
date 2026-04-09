// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.property

/** Extension for configuring Circuit project conventions. */
abstract class CircuitProjectExtension @Inject constructor(private val project: Project, objects: ObjectFactory, providers: ProviderFactory) {
  /** Whether Compose is enabled for this project. Defaults to true unless `circuit.noCompose` is set. */
  val hasCompose: Property<Boolean> = objects.property<Boolean>().convention(
    providers.provider {
      // project-local property, which providers.gradleProperty doesn't cover
      // TODO replace this with DSL calls in the project
      !project.hasProperty("circuit.noCompose") }
  )
}
