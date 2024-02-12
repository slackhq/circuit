plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.ksp)
  alias(libs.plugins.compose)
}

dependencies {
  implementation(projects.circuitCodegenAnnotations)
  implementation(projects.circuitFoundation)
  implementation(libs.kotlin.inject.runtime)

  implementation(compose.desktop.currentOs)
  implementation(libs.compose.foundation)
  implementation(libs.compose.runtime)
  implementation(libs.compose.material.material3)

  ksp(projects.circuitCodegen)
  ksp(libs.kotlin.inject.compiler.ksp)
}

//dependencies {
//  for (target in kspTargets) {
//    val targetConfigSuffix = if (target == "Metadata") "CommonMainMetadata" else target
//    add("ksp${targetConfigSuffix}", projects.circuitCodegen)
//  }
//}

ksp {
  arg("circuit.codegen.lenient", "true")
  arg("circuit.codegen.mode", "kotlin_inject")
}
