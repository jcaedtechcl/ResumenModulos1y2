plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "com.example.resumen_modulos1y2"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.resumen_modulos1y2"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // M3 ISS: la URL base se lee de gradle.properties y se publica como constante de BuildConfig.
        // Ninguna clase Kotlin la escribe a mano.
        val issBaseUrl = providers.gradleProperty("iss.baseUrl").get()
        buildConfigField("String", "ISS_BASE_URL", "\"$issBaseUrl\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Permite usar java.time (LocalDate) con minSdk 24: el código se adapta al compilar
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Room escribe aquí un JSON con el esquema de cada versión de la base de datos. Se sube al
// repositorio: es el historial de la base y lo usan las pruebas de migración (etapa 4).
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // room-testing (MigrationTestHelper) lee los esquemas JSON con kotlinx-serialization 1.8.1.
    // Navigation trae 1.7.3 y el APK de pruebas debe usar las mismas versiones que la app,
    // así que se sube aquí; si no, MigrationTestHelper falla con AbstractMethodError.
    implementation(libs.kotlinx.serialization.core)
    // M3 ISS: red. Retrofit 3 se compila contra OkHttp 4.12, por eso se usa esa versión.
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)
    // Solo en debug: el build de release ni siquiera contiene la clase HttpLoggingInterceptor.
    // Ver src/debug y src/release: RegistroDeRed.kt.
    debugImplementation(libs.okhttp.logging.interceptor)
    // M3 ISS: trabajo en segundo plano
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // Servidor HTTP falso para probar la capa de red sin internet (misma versión que OkHttp)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.work.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}