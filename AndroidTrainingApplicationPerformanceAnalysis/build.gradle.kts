// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidx.benchmark) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false

    // Plugin local (build-logic) que registra tasks atalho: microBench, macroBench, allBench.
    id("com.br.benchmark.shortcuts")
}