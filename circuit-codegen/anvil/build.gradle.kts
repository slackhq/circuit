
plugins{
    kotlin("jvm")
    alias(libs.plugins.ksp)
    alias(libs.plugins.mavenPublish)
}

dependencies {
    implementation(libs.autoService.annotations)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(projects.circuit)
    implementation(projects.circuitCodegen)
    implementation(libs.anvil.annotations)
    compileOnly(libs.ksp.api)
    ksp(libs.autoService.ksp)
}