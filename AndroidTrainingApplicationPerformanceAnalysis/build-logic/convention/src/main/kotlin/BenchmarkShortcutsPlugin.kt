import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Registra tasks "atalho" para rodar os benchmarks com nomes curtos, delegando
 * para as tasks longas geradas pelo Android Gradle Plugin.
 *
 * Deve ser aplicado no projeto RAIZ, pois as tasks reais vivem nos subprojetos
 * :benchmark:micro e :benchmark:macro.
 *
 * Atalhos criados:
 *   ./gradlew microBench  ->  :benchmark:micro:connectedReleaseAndroidTest
 *   ./gradlew macroBench  ->  :benchmark:macro:connectedBenchmarkAndroidTest
 *   ./gradlew allBench    ->  roda os dois acima
 */
class BenchmarkShortcutsPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "BenchmarkShortcutsPlugin deve ser aplicado no projeto raiz (build.gradle.kts raiz)."
        }

        val benchmarkGroup = "benchmark shortcuts"

        val microTaskPath = ":benchmark:micro:connectedReleaseAndroidTest"
        val macroTaskPath = ":benchmark:macro:connectedBenchmarkAndroidTest"

        val micro = target.tasks.register("microBench") {
            group = benchmarkGroup
            description = "Atalho para $microTaskPath (Microbenchmark)."
            dependsOn(microTaskPath)
        }

        val macro = target.tasks.register("macroBench") {
            group = benchmarkGroup
            description = "Atalho para $macroTaskPath (Macrobenchmark)."
            dependsOn(macroTaskPath)
        }

        target.tasks.register("allBench") {
            group = benchmarkGroup
            description = "Roda Micro e Macrobenchmark em sequência."
            dependsOn(micro, macro)
        }
    }
}
