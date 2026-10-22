# Configuração do Módulo de Macrobenchmark (`:benchmark:macro`)

Este documento registra o problema encontrado na configuração inicial do módulo
`:benchmark:macro`, a solução aplicada e um guia de como configurar corretamente
um módulo de Macrobenchmark neste projeto.

---

## 1. O problema

A configuração inicial do `benchmark/macro/build.gradle.kts` aplicava o plugin
**`com.android.library`** e tentava usar os atributos `targetProjectPath` e
`isDebuggable` próprios do fluxo de Macrobenchmark:

```kotlin
plugins {
    alias(libs.plugins.android.library)   // ❌ plugin errado para Macrobenchmark
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    // ...
    //targetProjectPath = ":app"           // comentado porque não compilava
    experimentalProperties["android.experimental.self-instrumenting"] = true
}
```

### Por que não funcionava

O Macrobenchmark funciona como um **APK de teste separado** que instala e
instrumenta o APK do `:app` **de fora** (em um processo externo). Esse modelo
exige o plugin **`com.android.test`**, não `com.android.library`.

| Atributo | Pertence a | Por que falhava |
| :--- | :--- | :--- |
| `targetProjectPath = ":app"` | `com.android.test` (`TestExtension`) | O DSL de `com.android.library` (`LibraryExtension`) **não possui** essa propriedade. Uma `library` é *consumida* por um app; ela não "aponta" para um app alvo. |
| `isDebuggable = false` (fluxo macro) | build type do `:app` | Para medições realistas, o app alvo precisa ser não-debuggable (próximo de release). Código debuggable desativa otimizações do ART e distorce os números. |

Resumo: o módulo estava configurado como uma **biblioteca comum**, e não como um
**módulo de teste de Macrobenchmark**. Por isso `targetProjectPath` precisava ficar
comentado — ele simplesmente não existe no DSL de `library`.

Um segundo problema surgiu após a troca do plugin: o uso simultâneo de
`targetProjectPath = ":app"` **e** `android.experimental.self-instrumenting = true`.
Esses dois modos são **mutuamente exclusivos**:

- `targetProjectPath` → instrumenta o **APK do `:app`** (dois APKs: alvo + teste).
- `self-instrumenting` → roda o benchmark **no próprio processo de teste**, sem app alvo.

Para medir o `:app` (startup, scroll jank, Baseline Profiles), o modelo correto é
`targetProjectPath`. O `self-instrumenting` deve ser removido.

### Terceiro problema: `kotlin.android` incompatível com AGP 9.0+

Após trocar o plugin e adicionar `org.jetbrains.kotlin.android` (para escrever os
benchmarks em Kotlin), o build passou a falhar com erro **fatal** na fase de
aplicação de plugins:

```
⛔ Failed to apply plugin 'org.jetbrains.kotlin.android'
The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support
since AGP 9.0.
Solution: Remove the 'org.jetbrains.kotlin.android' plugin from this project's
build file: benchmark/macro/build.gradle.kts.
```

Este projeto usa **AGP 9.0+**, que embute o suporte a Kotlin. Nessa versão o plugin
`kotlin.android` não é apenas desnecessário — ele é **proibido** e interrompe o build.
Por isso o módulo `:app` também mantém esse plugin comentado. A solução é **não**
aplicar `kotlin.android`; o suporte a Kotlin já vem do próprio AGP.

---

## 2. A solução

1. Trocar o plugin `com.android.library` por **`com.android.test`**.
2. **Não** aplicar `kotlin.android` (AGP 9.0+ embute Kotlin; aplicá-lo gera erro fatal).
3. Descomentar e manter **`targetProjectPath = ":app"`**.
4. **Remover** `experimentalProperties["android.experimental.self-instrumenting"]`.
5. Manter `isDebuggable = false` no build type `benchmark` do **`:app`** (não aqui).
6. Usar as dependências de Macrobenchmark (`benchmark-macro-junit4` + `uiautomator`).

### `benchmark/macro/build.gradle.kts` (final)

```kotlin
plugins {
    alias(libs.plugins.android.test)            // módulo de teste externo
    // AGP 9.0+ embute o suporte a Kotlin: NÃO aplicar kotlin.android (gera erro fatal).
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.br.macro"
    compileSdk { version = release(37) }

    defaultConfig {
        minSdk = 28 // Macrobenchmark recomenda API 28+ (ideal 29+ p/ System Tracing completo)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Suprime erros ao rodar em emulador durante o aprendizado.
        // Para números oficiais, remova e use aparelho físico.
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"

        // Rastreamento de recomposições do Compose no Perfetto.
        testInstrumentationRunnerArguments["androidx.benchmark.fullTracing.enable"] = "true"
    }

    buildTypes {
        // Deve ter o MESMO nome do buildType criado no :app
        create("benchmark") {
            signingConfig = getByName("debug").signingConfig
            matchingFallbacks += listOf("release")
        }
    }

    // Aponta qual aplicação será instalada e instrumentada externamente.
    // Obrigatório no plugin com.android.test.
    targetProjectPath = ":app"

    // NÃO usar "android.experimental.self-instrumenting" junto com targetProjectPath.

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

androidComponents {
    // Gera apenas a variante "benchmark" (evita compilar debug/release do módulo de teste).
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "benchmark"
    }
}

dependencies {
    // Em módulos com.android.test o código fica em src/main, então o scope é `implementation`.
    implementation(libs.androidx.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
```

---

## 3. Como configurar um módulo de Macrobenchmark corretamente

### Passo 1 — Plugin correto
Use `com.android.test`. Macrobenchmark **nunca** é `com.android.library`
(esse é o plugin do `:benchmark:micro`) nem `com.android.application`.

> ⚠️ **AGP 9.0+**: não aplique `org.jetbrains.kotlin.android`. O suporte a Kotlin é
> embutido no AGP a partir da versão 9.0, e aplicar o plugin gera erro fatal de build.

### Passo 2 — Apontar o app alvo
```kotlin
targetProjectPath = ":app"
```
Obrigatório. Define qual APK será instalado e instrumentado externamente.

### Passo 3 — Build type espelhando o `:app`
O módulo de teste precisa de um build type com o **mesmo nome** e
`matchingFallbacks` para o do `:app`:

```kotlin
// no :benchmark:macro
create("benchmark") {
    signingConfig = getByName("debug").signingConfig
    matchingFallbacks += listOf("release")
}
```

### Passo 4 — Configurar o build type no `:app`
É no `:app` que mora a configuração que garante medições válidas:

```kotlin
// no :app
create("benchmark") {
    initWith(buildTypes.getByName("release")) // otimizações de release
    signingConfig = signingConfigs.getByName("debug")
    matchingFallbacks += listOf("release")
    isDebuggable = false     // essencial p/ medições realistas
    isProfileable = true     // permite capturar traces no Perfetto
}
```

### Passo 5 — Ligar o Baseline Profile (opcional, mas recomendado)
No `:app`:
```kotlin
dependencies {
    implementation(libs.androidx.profileinstaller)
    "baselineProfile"(project(":benchmark:macro"))
}
```

### Passo 6 — Gerar apenas a variante de benchmark
```kotlin
androidComponents {
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "benchmark"
    }
}
```

### Passo 7 — Dependências
```kotlin
implementation(libs.androidx.benchmark.macro.junit4)
implementation(libs.androidx.test.uiautomator)
implementation(libs.androidx.junit)
```

---

## 4. Checklist de validação

| Item | Onde | Valor esperado |
| :--- | :--- | :--- |
| Plugin | `:benchmark:macro` | `com.android.test` |
| `kotlin.android` | `:benchmark:macro` | ausente (AGP 9.0+) |
| `targetProjectPath` | `:benchmark:macro` | `":app"` |
| `self-instrumenting` | `:benchmark:macro` | ausente |
| build type `benchmark` + `matchingFallbacks` | `:benchmark:macro` e `:app` | mesmo nome |
| `isDebuggable` | `:app` (build type benchmark) | `false` |
| `isProfileable` | `:app` (build type benchmark) | `true` |
| `benchmark-macro-junit4` | `:benchmark:macro` | presente |

---

## 5. Por que não `self-instrumenting`?

| Modo | Quando usar | APKs |
| :--- | :--- | :--- |
| `targetProjectPath` | Medir um app real do projeto (startup, jank, baseline) | 2 (alvo + teste) |
| `self-instrumenting` | Benchmark sem app alvo separado (casos isolados/experimentais) | 1 |

Como o objetivo aqui é medir o `:app`, o modo correto é `targetProjectPath`.

---

## Referências

- [Macrobenchmark — documentação oficial](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview)
- [Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/overview)
