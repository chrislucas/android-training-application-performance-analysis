# android-training-application-performance-analysis
Um app para praticar análise de performance, usando micro, macro benchmarks e as ferramentas de profilling do android studio

## Como validar se o setup está funcionando

### No Android Studio

Abra a aba **Build Variants** (menu lateral esquerdo ou `View > Tool Windows > Build Variants`) e selecione as variantes conforme o tipo de benchmark:

- **Macrobenchmark:** selecione a variante `benchmark` para `:app` e `:benchmark:macro`.
  (O módulo `:benchmark:macro` só habilita a variante `benchmark`, que espelha o build type `benchmark` do `:app`.)
- **Microbenchmark:** selecione `release` para `:benchmark:micro`.
  (O módulo está configurado com `testBuildType = "release"` para rodar com as otimizações reais do R8/ART.)

### Via linha de comando

Os relatórios JSON e os traces do Perfetto são gerados em:

```
build/outputs/connected_android_test_additional_output/
```

#### Atalhos (recomendado)

O módulo `build-logic` registra tasks atalho com nomes curtos para rodar os benchmarks:

```bash
./gradlew microBench   # Microbenchmark
./gradlew macroBench   # Macrobenchmark
./gradlew allBench     # roda Micro e Macrobenchmark em sequência
```

Cada atalho delega para a task completa correspondente (ver abaixo).

#### Comandos completos

- **Microbenchmark:**
  ```bash
  ./gradlew :benchmark:micro:connectedReleaseAndroidTest
  ```
- **Macrobenchmark:**
  ```bash
  ./gradlew :benchmark:macro:connectedBenchmarkAndroidTest
  ```

> É necessário um dispositivo físico ou emulador conectado. Para medições oficiais, prefira um aparelho físico; em emulador, os módulos já suprimem os erros correspondentes via `androidx.benchmark.suppressErrors`.
