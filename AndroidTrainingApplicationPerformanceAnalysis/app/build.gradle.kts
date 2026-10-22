plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    //alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.br.androidtrainingapplicationperformanceanalysis"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.br.androidtrainingapplicationperformanceanalysis"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Facilita usar o CPU/Memory Profiler sem precisar de build release,
            // embora para medições reais de tempo o buildType 'benchmark' seja o correto.
            isProfileable = true
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        // BuildType exclusivo para o Macrobenchmark
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
            isProfileable = true
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:engine"))
    implementation(project(":core:database"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Permite ver os nomes das funções @Composable nas traces do Perfetto!
    implementation(libs.androidx.compose.runtime.tracing)

    implementation(libs.androidx.work.runtime.ktx)

    // Necessário para aplicar Baseline Profiles e habilitar medições precisas de Startup
    implementation(libs.androidx.profileinstaller)

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")


    // Conecta o gerador de Baseline Profile do módulo :benchmark:macro
    "baselineProfile"(project(":benchmark:macro"))

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}