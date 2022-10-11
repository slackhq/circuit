plugins {
  id("com.android.application")
}

android {
  namespace = "com.slack.circuit.sample.star.apk"
  defaultConfig {
    minSdk = 28
    targetSdk = 33
    versionCode = 1
    versionName = "1"
  }
}

dependencies {
  implementation(projects.samples.star)
}