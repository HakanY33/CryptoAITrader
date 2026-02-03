package com.example.cryptopredictionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import com.example.cryptopredictionapp.ui.MainScreen
import com.example.cryptopredictionapp.ui.theme.CryptoPredictionAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Tema ile sarmalıyoruz ki arka plan ve renkler çalışsın
            CryptoPredictionAppTheme {
                MainScreen() // Artık ui/MainScreen.kt içindeki havalı ekranı çağırıyor
            }
        }
    }
}


