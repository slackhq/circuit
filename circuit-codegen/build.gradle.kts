plugins {
    kotlin("jvm")
    alias(libs.plugins.ksp)
    alias(libs.plugins.mavenPublish)
}

dependencies {
    implementation(libs.autoService.annotations)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(projects.circuit)

    compileOnly(libs.ksp.api)
    ksp(libs.autoService.ksp)

}