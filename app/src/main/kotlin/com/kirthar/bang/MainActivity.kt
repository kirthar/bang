package com.kirthar.bang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kirthar.bang.navigation.BangApp

/**
 * Actividad única de la aplicación (apaisada, forzada en el manifest). Toda la UI es
 * Compose: [BangApp] contiene el tema y la navegación entre las 4 pantallas
 * (menú -> configuración -> partida -> fin de partida).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BangApp()
        }
    }
}
