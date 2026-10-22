# Projeto JankyPulse


Para criar um laboratório completo de engenharia 
de performance — seguindo o ciclo Measure → Identify → Optimise → Validate do post — 
o melhor cenário é construir o "JankyPulse": um dashboard financeiro e de telemetria em tempo real.  

Para suportar tanto o Macrobenchmark (que exige rodar em um processo separado, 
instrumentando o APK de fora) quanto o Microbenchmark (que precisa testar funções puras de 
CPU/memória isoladamente sem overhead da UI), a arquitetura deve ser multi-modular.


# 1. Diagrama de Módulos e Pacotes

```text
📦 JankyPulse-Project (Root)
│
├── 📁 :app (com.jankypulse.app) -> [Application Module]
│   ├── 📁 di/                      # Injeção de dependências (com inicializações bloqueantes propositais)
│   ├── 📁 telemetry/               # Workers, Wakelocks e Location/Network Polling (Gargalos de Bateria/Rede)
│   │   ├── RogueSyncWorker.kt      # WorkManager mal configurado + Polling infinito
│   │   └── LeakySensorManager.kt   # Listener estático segurando referência de Context/Activity
│   ├── 📁 ui/                      # Jetpack Compose (Gargalos de UI Jank, Recomposições e Layout)
│   │   ├── navigation/
│   │   ├── dashboard/              # Tela principal com animações pesadas e listas sem chave (key)
│   │   │   ├── DashboardScreen.kt
│   │   │   ├── DashboardViewModel.kt
│   │   │   └── JankyChartCanvas.kt # Alocação de objetos (Paint/Path) dentro do onDraw
│   │   └── components/             # Componentes instáveis (usando List em vez de ImmutableList)
│   └── JankyApplication.kt         # Ponto de partida com I/O pesado na Main Thread (Gargalo de Cold Start)
│
├── 📁 :core:engine (com.jankypulse.core.engine) -> [Android Library Module]
│   ├── 📁 crypto/                  # Algoritmos pesados de hash/criptografia (Alvo de CPU Profiler & Microbenchmark)
│   ├── 📁 parser/                  # Deserialização JSON manual ineficiente e Regex não compilado
│   └── 📁 math/                    # Cálculos de médias móveis O(N²) com boxing de primitivos (Autoboxing Thrashing)
│
├── 📁 :core:database (com.jankypulse.core.database) -> [Android Library Module]
│   ├── 📁 dao/                     # Queries N+1, falta de @Transaction e ausência de índices (SQLite/Room)
│   ├── 📁 entity/                  # Tabelas sem Index nas Foreign Keys / colunas de busca
│   └── PulseDatabase.kt            # Configurado propositalmente com .allowMainThreadQueries()
│
├── 📁 :benchmark:macro (com.jankypulse.benchmark.macro) -> [com.android.test Module]
│   ├── StartupBenchmark.kt         # Mede Time To Initial Display (TTID) e Time To Full Display (TTFD)
│   ├── ScrollJankBenchmark.kt      # Mede FrameTimingMetric (Jank / Frame Overrun) na lista e animações
│   └── BaselineProfileGenerator.kt # Geração de Baseline Profiles para otimização AOT
│
└── 📁 :benchmark:micro (com.jankypulse.benchmark.micro) -> [com.android.library + androidx.benchmark]
    └── src/androidTest/
        ├── CryptoEngineBenchmark.kt # Mede ciclos de CPU e alocações no :core:engine:crypto
        └── MathAggrBenchmark.kt     # Compara performance antes/depois de remover Autoboxing e O(N²)
```

# 2. Mapa de "Sabotagens" vs. Ferramentas de Troubleshooting

Cada pacote acima foi desenhado para engatilhar um alerta específico nas ferramentas do 
Android Studio e do Jetpack Benchmark:


| Pacote / Classe | O Problema Plantado (Sabotagem) | Onde Medir e Identificar | Como Otimizar e Validar |
| :--- | :--- | :--- | :--- |
| **JankyApplication.kt** | Leitura síncrona de SharedPreferences, parse de JSON grande e inicialização de SDKs no `onCreate()`. | Macrobenchmark (`StartupTimingMetric`) + Perfetto (System Trace no início do processo). | Mover para App Startup library / Coroutines em `Dispatchers.IO` e gerar Baseline Profile. |
| **ui.dashboard** | Animação infinita recalculando estado a cada frame sem `remember`; `LazyColumn` lendo listas mutáveis (`List<T>`) e recriando `Paint()` dentro do Canvas. | Layout Inspector (contagem de recomposições), Macrobenchmark (`FrameTimingMetric`) e System Trace. | Usar `@Immutable`, `derivedStateOf`, `Modifier.drawWithCache` e ler estado na fase de Draw (`graphicsLayer`). |
| **core.database** | Queries na Main Thread, busca textual com `LIKE %...%` sem FTS, inserção de 5.000 itens em loop sem transação única. | App Inspection (Database Inspector) + CPU Profiler (bloqueio da Main Thread por `SQLiteConnection`). | Adicionar `@Index`, usar `@Transaction` em batch inserts, tirar da Main Thread com `Flow`/`Dispatchers.IO`. |
| **core.engine.math** | Cálculo de indicadores financeiros usando `ArrayList<Double>` (boxing pesado) e loops aninhados $O(N^2)$ gerando Garbage Collection constante. | Microbenchmark (com flag de alocação ativa) + Memory Profiler (padrão "dente de serra" / GC thrashing). | Trocar para `DoubleArray` primitivo, reduzir complexidade para $O(N)$ com janela deslizante. |
| **telemetry.** | `LeakySensorManager` Singleton registrando uma Activity ou View em um callback de atualização de cotação e esquecendo de limpar no `onCleared`/`onDestroy`. | Memory Profiler (Heap Dump filtrando por Activity/Fragment Leaks) + LeakCanary. | Usar `WeakReference`, ou amarrar o coletor ao ciclo de vida via `repeatOnLifecycle(Lifecycle.State.STARTED)`. |
| **telemetry.RogueSyncWorker** | Loop de polling HTTP a cada 2 segundos sem cache, acordando o rádio do aparelho e segurando um `PowerManager.WakeLock` sem timeout. | Energy Profiler / Background Task Inspector + Network Inspector. | Implementar cache HTTP/paginação, exponential backoff e usar `WorkManager` com constraints adequadas. |


# 3. Por que separar :core:engine para o Microbenchmark?

O **Macrobenchmark** roda como um teste instrumentado externo que inicializa o seu `:app`
inteiro compilado em modo benchmark (próximo ao release, com `isDebuggable = false`),
sendo ideal para medir Startup e UI Jank.

Já o **Microbenchmark** roda dentro do código de teste de uma biblioteca (`com.android.library`)
e executa um loop milhares de vezes diretamente na função alvo para medir nanosegundos de CPU
e alocações de memória.

Ao isolar `:core:engine` e `:core:database` como módulos de biblioteca, você consegue
plugá-los diretamente no `:benchmark:micro` sem carregar o peso da UI do módulo `:app`.

---

**Próximo passo:** quer que o próximo passo seja configurar os arquivos `build.gradle.kts`
(incluindo o buildType `benchmark` e os plugins do Micro/Macrobenchmark) ou prefere começar
escrevendo o código "sabotado" de algum desses módulos?


