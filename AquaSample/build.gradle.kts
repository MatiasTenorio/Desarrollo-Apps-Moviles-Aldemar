// AGP 9 trae por defecto el plugin de Kotlin (KGP) 2.2.10. Algunas librerías
// (Room, WorkManager, Coil...) ya vienen compiladas con Kotlin 2.4, y un
// compilador 2.2 no puede leerlas. Así se usa un KGP más nuevo, según la
// documentación de AGP 9. Debe ser la misma versión que "kotlin" en
// gradle/libs.versions.toml.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}