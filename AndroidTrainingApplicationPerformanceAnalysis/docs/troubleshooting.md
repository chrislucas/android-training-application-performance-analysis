# Troubleshooting

Registro de problemas de build/configuração e suas soluções.

---

## `Unresolved reference 'enableUnitTest'` ao configurar o `:app`

### Sintoma

Qualquer comando que configure o módulo `:app` (sync do Android Studio,
`./gradlew tasks`, `./gradlew :app:assembleDebug`, etc.) falhava na **compilação
do próprio build script** `app/build.gradle.kts`, antes de qualquer task rodar:

```
w: file:///.../app/build.gradle.kts:92:13: Check for instance is always 'true'.
e: file:///.../app/build.gradle.kts:93:28: Unresolved reference 'enableUnitTest'.

> Build file '.../app/build.gradle.kts' line: 92
  Line 92:         if (variantBuilder is com.android.build.api.variant.HasUnitTestBuilder) {
  Line 93:             variantBuilder.enableUnitTest = true
                                      ^ Unresolved reference 'enableUnitTest'.
BUILD FAILED
```

### Código que causava o erro

```kotlin
androidComponents {
    beforeVariants(selector().withBuildType("release")) { variantBuilder ->
        if (variantBuilder is com.android.build.api.variant.HasUnitTestBuilder) {
            variantBuilder.enableUnitTest = true
        }
    }
}
```

### Causa raiz

**Não era a versão do AGP.** O erro era idêntico no AGP `9.5.0-alpha10` (versão do
projeto) e também numa versão estável (`9.3.3`), o que descarta a hipótese de
incompatibilidade de versão ou de metadata Kotlin. A interface
`com.android.build.api.variant.HasUnitTestBuilder` existe e expõe
`var enableUnitTest: Boolean` em todas essas versões (confirmado via `javap` e
pelos fontes do AGP).

O problema é uma limitação de resolução do **compilador de build scripts** — o
Kotlin embarcado no Gradle (Gradle 9.6.0 → Kotlin 2.3.21), que é diferente do
Kotlin do projeto (2.4.21):

1. O parâmetro `variantBuilder` da lambda de `beforeVariants` tem tipo estático
   `ApplicationVariantBuilder`.
2. `ApplicationVariantBuilder` **já herda** `HasUnitTestBuilder`.
3. Por isso o compilador considera `variantBuilder is HasUnitTestBuilder`
   "sempre true" (a warning da linha 92) e **descarta o smart-cast**.
4. Sem o smart-cast ativo, a resolução do membro falha nesse contexto e tanto
   `enableUnitTest` quanto `setEnableUnitTest` ficam como *unresolved reference*.

### Solução

Atribuir `variantBuilder` a uma **variável explicitamente tipada** como
`HasUnitTestBuilder`. Isso força a resolução do membro e dispensa o `if (is ...)`
(que era redundante, já que o tipo sempre implementa a interface):

```kotlin
androidComponents {
    beforeVariants(selector().withBuildType("release")) { variantBuilder ->
        val b: com.android.build.api.variant.HasUnitTestBuilder = variantBuilder
        b.enableUnitTest = true
    }
}
```

### Por que reabilitar o unit test da variante `release`?

A partir do AGP 9.0, a task de unit test é criada apenas para o `testBuildType`
(por padrão, `debug`); a variante `release` fica com `enableUnitTest = false`.

A feature de Compose Preview do AGP 9 (`generateReleaseComposePreviewRunfiles`)
exige unit tests habilitados na variante para empacotar os recursos XML/temas
destinados ao Layoutlib. Sem isso, qualquer comando que configure a variante
`release` do `:app` falha com:

```
Unit tests are disabled for this variant (enableUnitTest = false in beforeVariants).
```

Reabilitar o unit test da variante `release` destrava a configuração do projeto.

### Verificação

```bash
./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

### Observação sobre versões

O projeto permanece no AGP `9.5.0-alpha10` **de propósito**: ele usa APIs
experimentais desse preview que quebram em versões estáveis, por exemplo:

- `release { optimization { packageScope = setOf(...) } }` — exige a flag
  experimental `android.r8.gradual.support` e só está disponível no preview.
- `compileSdk { version = release(37) }`.

Migrar para uma versão estável do AGP exigiria reescrever esses pontos e **não é
necessário** para corrigir o erro de `enableUnitTest`.
