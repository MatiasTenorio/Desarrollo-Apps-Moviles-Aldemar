import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
}

val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val apiBaseUrl: String = propiedadesLocales.getProperty("api.baseUrl") ?: "http://10.0.2.2:8080/"

val insforgeBaseUrl: String = propiedadesLocales.getProperty("insforge.baseUrl") ?: ""
val insforgeAnonKey: String = propiedadesLocales.getProperty("insforge.anonKey") ?: ""


val propiedadesFirma = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

android {
    namespace = "com.duoc.aquasample"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.duoc.aquasample"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "INSFORGE_BASE_URL", "\"$insforgeBaseUrl\"")
        buildConfigField("String", "INSFORGE_ANON_KEY", "\"$insforgeAnonKey\"")
    }

    signingConfigs {
        if (propiedadesFirma.containsKey("storeFile")) {
            create("release") {
                storeFile = file(propiedadesFirma.getProperty("storeFile"))
                storePassword = propiedadesFirma.getProperty("storePassword")
                keyAlias = propiedadesFirma.getProperty("keyAlias")
                keyPassword = propiedadesFirma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.navigation.compose)

    // Persistencia local (Room)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Fotos: CameraX (captura), Coil (mostrar), Telephoto (zoom)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.coil.compose)
    implementation(libs.telephoto.zoomable.image.coil3)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Backend: Retrofit (API REST) y WorkManager (sincronización en segundo plano)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.androidx.work.runtime.ktx)

    // Login: Insforge (autenticación con correo y contraseña)
    implementation(libs.insforge.kotlin)

    // Pruebas unitarias
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

/*
 * Cobertura con Kover. Reporte:  .\gradlew.bat koverHtmlReportDebug
 * Verificación del 90%:          .\gradlew.bat koverVerifyDebug
 *
 * Se mide la lógica evaluable (ViewModels, repositorios, validaciones, cálculos).
 * Se excluye lo que no se puede probar con pruebas unitarias JVM: pantallas
 * Compose (se prueban con Compose UI Test en androidTest), código generado por
 * Room, y clases que solo conectan Android (Activity, Application, WorkManager,
 * cámara, archivos, PDF).
 */
kover {
    reports {
        filters {
            excludes {
                annotatedBy(
                    "androidx.compose.runtime.Composable",
                    "androidx.compose.ui.tooling.preview.Preview"
                )
                classes(
                    "*ComposableSingletons*",
                    "*_Impl",
                    "*_Impl\$*",
                    "*.BuildConfig",
                    "*.R",
                    "*.R\$*",
                    "com.duoc.aquasample.MainActivity",
                    "com.duoc.aquasample.AquaSampleApplication",
                    "com.duoc.aquasample.data.AppContainer*",
                    "com.duoc.aquasample.data.local.AquaSampleDatabase*",
                    "com.duoc.aquasample.data.repository.FotoRepositoryImpl*",
                    "com.duoc.aquasample.data.sync.SincronizacionWorker*",
                    "com.duoc.aquasample.data.sync.ProgramadorSincronizacion*",
                    "com.duoc.aquasample.data.sync.AlmacenFotosAndroid*",
                    "com.duoc.aquasample.data.reporte.GeneradorReportePdf*",
                    "com.duoc.aquasample.ui.ViewModelExtrasKt",
                    "*ViewModel\$Companion*",
                    "com.duoc.aquasample.ui.theme.*",
                    "com.duoc.aquasample.ui.camera.*",
                    // Archivos de pantallas: solo dibujan UI (constantes de color, formas, Canvas).
                    "com.duoc.aquasample.ui.*.*ScreenKt*",
                    "com.duoc.aquasample.ui.dashboard.GraficoBarrasKt*",
                    "com.duoc.aquasample.ui.history.HistoryFiltrosKt*",
                    "com.duoc.aquasample.ui.components.*",
                    "com.duoc.aquasample.ui.navigation.*",
                    "com.duoc.aquasample.ui.postsampleform.*"
                )
            }
        }
        verify {
            rule {
                minBound(90)
            }
        }
    }
}