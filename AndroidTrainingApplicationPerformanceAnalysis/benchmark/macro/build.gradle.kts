plugins {
    alias(libs.plugins.android.test)
    // AGP 9.0+ tem suporte a Kotlin embutido; o plugin kotlin.android não deve ser aplicado.
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.br.macro"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 28 // Macrobenchmark recomenda API 28+ (ideal 29+ para System Tracing completo)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Suprime erros caso você queira rodar em emulador durante o aprendizado
        // (Para números oficiais de produção, remova isso e use um aparelho físico)
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"

        // Habilita rastreamento de recomposições do Compose no Perfetto durante o Macrobenchmark
        testInstrumentationRunnerArguments["androidx.benchmark.fullTracing.enable"] = "true"
    }

    buildTypes {
        // Deve ter o mesmo nome do buildType criado no :app
        create("benchmark") {
           // isDebuggable = true
            signingConfig = getByName("debug").signingConfig
            matchingFallbacks += listOf("release")
        }
    }


    // Aponta qual aplicação será instalada e instrumentada externamente pelo Macrobenchmark.
    // Obrigatório no plugin com.android.test.
    targetProjectPath = ":app"

    // OBS: NÃO usar "android.experimental.self-instrumenting" em conjunto com targetProjectPath.
    // Self-instrumenting roda o benchmark no próprio processo de teste (sem app alvo externo),
    // o que é mutuamente exclusivo com a instrumentação externa do :app usada aqui.

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

androidComponents {
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "benchmark"
    }
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}