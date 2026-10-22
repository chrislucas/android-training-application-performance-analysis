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

    /*
        https://developer.android.com/training/testing/local-tests/robolectric#ui-testing
        Robolectric can also run UI tests such as Espresso or Compose tests.
        You can convert an instrumented test to Robolectric by moving it to the test
        source set and setting up the Robolectric dependencies.
     */
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true // útil para mocks do Android
        }
    }

    buildFeatures {
        compose = true
    }
}

/*
 * A partir do AGP 9.0, a task de unit test é criada apenas para o `testBuildType`
 * (por padrão, `debug`). Com isso, a variante `release` fica com `enableUnitTest = false`.
 *
 * A feature de Compose Preview do AGP 9 (`generateReleaseComposePreviewRunfiles`)
 * exige unit tests habilitados na variante para empacotar os recursos XML/temas
 * destinados ao Layoutlib. Sem isso, qualquer comando que configure a variante
 * `release` do `:app` (ex.: `./gradlew tasks` ou o sync do Android Studio) falha com:
 *   "Unit tests are disabled for this variant (enableUnitTest = false in beforeVariants)".
 *
 * Reabilitamos o unit test da variante `release` para destravar a configuração do projeto.
 */
androidComponents {
    beforeVariants(selector().withBuildType("release")) { variantBuilder ->
        // `ApplicationVariantBuilder` já implementa `HasUnitTestBuilder`, então um
        // `if (variantBuilder is HasUnitTestBuilder)` é considerado "sempre true" pelo
        // compilador de build scripts (Kotlin embarcado no Gradle), que acaba descartando
        // o smart-cast e deixando `enableUnitTest` como "unresolved reference".
        // Atribuir a uma variável explicitamente tipada força a resolução do membro.
        val b: com.android.build.api.variant.HasUnitTestBuilder = variantBuilder
        b.enableUnitTest = true
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

    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Jetpack Compose Test para JUnit4 e JVM
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest) // Necessário se carregar Activities/Componentes

    // Robolectric
    testImplementation("org.robolectric:robolectric:4.17") // Verifique a versão mais recente compatível
    testImplementation("androidx.test:core:1.7.0")
    testImplementation(libs.androidx.junit)

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