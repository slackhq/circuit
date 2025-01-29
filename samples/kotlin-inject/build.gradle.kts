// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.google.devtools.ksp.KspExperimental

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.ksp)
  alias(libs.plugins.compose)
  alias(libs.plugins.metro)
}

ksp {
  arg("circuit.codegen.lenient", "true")
  arg("circuit.codegen.mode", "metro")
  @OptIn(KspExperimental::class)
  // kotlin-inject cannot see through typealiases in KSP2
  // https://github.com/evant/kotlin-inject/issues/458
  useKsp2.set(false)
}

metro {
  customAnnotations {
    includeKotlinInject()
    includeAnvil()
  }
}

dependencies {
  ksp(projects.circuitCodegen)
//  ksp(libs.kotlinInject.compiler)
//  ksp(libs.kotlinInject.anvil.compiler)

  implementation(projects.circuitCodegenAnnotations)
  implementation(projects.circuitFoundation)

  implementation(compose.desktop.currentOs)
  implementation(libs.compose.foundation)
  implementation(libs.compose.runtime)
  implementation(libs.compose.material.material3)

  implementation(libs.kotlinInject.runtime)
  implementation(libs.kotlinInject.anvil.runtime)
  implementation(libs.kotlinInject.anvil.runtime.optional)
}
