// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.ksp)
  alias(libs.plugins.compose)
}

ksp {
  arg("circuit.codegen.lenient", "true")
  arg("circuit.codegen.mode", "kotlin_inject_anvil")
}

dependencies {
  ksp(projects.circuitCodegen)
  ksp(libs.kotlinInject.compiler)
  ksp(libs.kotlinInject.anvil.compiler)

  implementation(projects.circuitCodegenAnnotations)
  implementation(projects.circuitFoundation)

  implementation(compose.desktop.currentOs)
  implementation(libs.compose.foundation)
  implementation(libs.compose.runtime)
  implementation(libs.compose.material.material3)

  implementation(libs.kotlinInject.runtime)
  implementation(libs.kotlinInject.anvil.runtime)
}
