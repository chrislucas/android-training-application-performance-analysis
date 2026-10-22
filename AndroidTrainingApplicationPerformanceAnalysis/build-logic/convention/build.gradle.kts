plugins {
    `kotlin-dsl`
}

group = "com.br.buildlogic"

gradlePlugin {
    plugins {
        // Plugin que registra tasks "atalho" para rodar os benchmarks com nomes curtos.
        register("benchmarkShortcuts") {
            id = "com.br.benchmark.shortcuts"
            implementationClass = "BenchmarkShortcutsPlugin"
        }
    }
}
