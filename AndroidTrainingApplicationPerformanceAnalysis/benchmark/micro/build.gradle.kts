/*
    Ao contrário do Macro, o Microbenchmark é um módulo com.android.library com o
    plugin androidx.benchmark. Ele usa um AndroidBenchmarkRunner especial que aquece a JVM/ART,
    trava o clock da CPU (se o device tiver root) e mede nanosegundos e alocações de memória por operação:
 */

plugins {
    alias(libs.plugins.android.library)
    /*
         Por que não consigo adicionar alias(libs.plugins.kotlin.android) ao módulo
         benchmark:micro?

           A causa é o AGP 9.0+ com suporte a Kotlin embutido. A partir dessa versão,
           o Android Gradle Plugin já aplica internamente o suporte a Kotlin a qualquer módulo
           'com.android.application, com.android.library ou com.android.test".

           Aplicar manualmente org.jetbrains.kotlin.android em cima disso gera o mesmo
           erro fatal que você viu no macro:
     */

    //alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.benchmark)
}

android {
    namespace = "com.br.micro"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
        // Runner específico do Microbenchmark!
        testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"

        // Permite rodar em emulador ou dispositivo sem clock travado para fins de estudo
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR,LOW-BATTERY,UNLOCKED"

        // Ativa a contagem de alocações de memória junto com o tempo de CPU
        testInstrumentationRunnerArguments["androidx.benchmark.profiling.mode"] = "StackSampling"
    }

    testBuildType = "release"

    buildTypes {
        debug {
            // Microbenchmarks rodam no buildType 'release' da library para ter otimizações reais do R8/ART
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "benchmark-proguard-rules.pro"
            )
        }
        release {
            isDefault = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    /*
        kotlinOptions { jvmTarget = "17" } é equivalente a kotlinOptions { jvmTarget = "17" } ?
        Sim
     */
    // https://developer.android.com/kotlin/multiplatform/plugin#configure-jvm-target
    kotlin {
        jvmToolchain(17)
    }

}

dependencies {
    // Importa os módulos onde estão os algoritmos que vamos "sabotar" e otimizar
    androidTestImplementation(project(":core:engine"))
    androidTestImplementation(project(":core:database"))

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.benchmark.junit4)
}