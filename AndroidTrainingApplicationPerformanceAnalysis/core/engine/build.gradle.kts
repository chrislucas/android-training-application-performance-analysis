plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.br.engine"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}