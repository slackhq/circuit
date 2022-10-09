plugins {
  kotlin("jvm")
  alias(libs.plugins.mosaic)
  `application`
}

application {
  mainClass.set("com.slack.circuit.sample.counter.mosaic.MosaicCounterCircuitKt")
}

dependencies {
  implementation(projects.samples.counter)
  implementation(projects.circuit)
  implementation(libs.clikt)
  implementation(libs.jline)
}