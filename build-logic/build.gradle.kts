plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.plugin.sam)
  id("java-gradle-plugin")
}

// To keep kotlin-dsl-esque DSL APIs
samWithReceiver { annotation("org.gradle.api.HasImplicitReceiver") }

gradlePlugin {
  plugins {
    register("base") {
      id = "circuit.base"
      implementationClass = "com.slack.circuit.gradle.CircuitBasePlugin"
    }
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().removeSuffix("-ea").toInt()))
  }
}

kotlin { compilerOptions { allWarningsAsErrors.set(true) } }

dependencies {
  compileOnly(gradleApi())
  api(libs.gradlePlugins.agp)
  api(libs.gradlePlugins.spotless)
  api(libs.gradlePlugins.dependencyGuard)
  api(libs.gradlePlugins.anvil)
  api(libs.gradlePlugins.mavenPublish)
  api(libs.gradlePlugins.detekt)
  api(libs.gradlePlugins.binaryCompatibilityValidator)
  api(libs.gradlePlugins.dokka)
  api(libs.gradlePlugins.composeCompiler)
  api(libs.gradlePlugins.kotlin)
  api(libs.gradlePlugins.emulatorWtf)

  // Expose the generated version catalog API to the plugins.
  implementation(files(libs::class.java.superclass.protectionDomain.codeSource.location))
}
