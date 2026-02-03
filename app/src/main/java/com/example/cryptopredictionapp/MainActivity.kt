package com.example.cryptopredictionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cryptopredictionapp.ui.screens.HomeScreen
import com.example.cryptopredictionapp.ui.screens.AutoTraderScreen
import com.example.cryptopredictionapp.ui.viewmodel.CryptoViewModel
import com.example.cryptopredictionapp.ui.viewmodel.AutoTraderViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    // Ekran Kontrolü (0: Home, 1: AutoBot)
    var selectedTab by remember { mutableStateOf(0) }

    // ÖNEMLİ: İki ekranın da konuşacağı ORTAK beyin (ViewModel)
    val sharedCryptoViewModel: CryptoViewModel = viewModel()
    // AutoTrader ViewModel'i
    val autoTraderViewModel: AutoTraderViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Analiz") },
                    label = { Text("Analiz") },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFFE3F2FD))
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Avcı") },
                    label = { Text("Otomatik Avcı") },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFFE3F2FD))
                )
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            if (selectedTab == 0) {
                // Home ekranına ortak ViewModel'i veriyoruz
                HomeScreen(viewModel = sharedCryptoViewModel)
            } else {
                // AutoTrader ekranı
                AutoTraderScreen(
                    viewModel = autoTraderViewModel,
                    onInspectClick = { symbol ->
                        // 1. Home ekranındaki coini güncelle
                        sharedCryptoViewModel.onCoinSelected(symbol)
                        // 2. Home sekmesine (Analiz) geçiş yap
                        selectedTab = 0
                    }
                )
            }
        }
    }
}