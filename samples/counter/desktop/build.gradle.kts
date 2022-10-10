import org.jetbrains.compose.desktop.preview.tasks.AbstractConfigureDesktopPreviewTask
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME

plugins {
  kotlin("jvm")
  alias(libs.plugins.compose)
  `java-library`
}

compose.desktop {
  application {
    mainClass = "com.slack.circuit.sample.counter.desktop.DesktopCounterCircuitKt"
  }
}

tasks.withType<AbstractConfigureDesktopPreviewTask>().configureEach {
  notCompatibleWithConfigurationCache("https://github.com/JetBrains/compose-jb/issues/2376")
}

dependencies {
  implementation(compose.desktop.currentOs)
  implementation(projects.samples.counter)
  implementation(projects.circuit)
  add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, libs.androidx.compose.compiler)
}
