package com.example.cryptopredictionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.cryptopredictionapp.ui.screens.HomeScreen
import com.example.cryptopredictionapp.ui.theme.CryptoPredictionAppTheme // Bu isim senin projene göre değişebilir, kırmızı yanarsa silip Alt+Enter yap

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CryptoPredictionAppTheme {
                HomeScreen()
            }
        }
    }
}