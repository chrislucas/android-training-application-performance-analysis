package com.br.androidtrainingapplicationperformanceanalysis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.br.androidtrainingapplicationperformanceanalysis.ui.theme.AndroidTrainingApplicationPerformanceAnalysisTheme

/*
    Fonte do projeto
    https://gemini.google.com/app/df4358c6971dfef3

    construir um projeto onde eu possa praticar essas analises de troubleshoot.
    A ideia é construir algo que tenha uma serie de problemas de performance,
    como animacoes, chamadas ao banco, uso excessivo de bateria e memoria e ir corrigindo ponto
     a ponto para aprender a mexer com as ferramentas de profile da IDE.
     Quero incluir nesse projeto o uso de micro e macro benchmark

    https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview
    https://developer.android.com/topic/performance/benchmarking/microbenchmark-overview

    Profile your app performance
    https://developer.android.com/studio/profile

 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidTrainingApplicationPerformanceAnalysisTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidTrainingApplicationPerformanceAnalysisTheme {
        Greeting("Android")
    }
}