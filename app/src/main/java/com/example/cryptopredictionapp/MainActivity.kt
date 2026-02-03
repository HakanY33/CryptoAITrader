package com.example.cryptopredictionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings // Şimdilik Settings ikonunu kullanalım
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.cryptopredictionapp.ui.screens.HomeScreen
import com.example.cryptopredictionapp.ui.screens.AutoTraderScreen

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
    // Hangi ekrandayız? (0: Home, 1: AutoBot)
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                // 1. Sekme: Analiz (Home)
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Analiz") },
                    label = { Text("Analiz") }
                )
                // 2. Sekme: Otomatik Avcı
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Avcı") },
                    label = { Text("Otomatik Avcı") }
                )
            }
        }
    ) { innerPadding ->
        // Ekran Değiştirme Mantığı
        if (selectedTab == 0) {
            // Home ekranına padding verip çağırıyoruz
            Surface(modifier = Modifier.padding(innerPadding)) {
                HomeScreen()
            }
        } else {
            // AutoTrader ekranı
            Surface(modifier = Modifier.padding(innerPadding)) {
                AutoTraderScreen()
            }
        }
    }
}