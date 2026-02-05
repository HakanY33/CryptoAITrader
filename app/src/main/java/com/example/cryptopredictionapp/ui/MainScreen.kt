package com.example.cryptopredictionapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cryptopredictionapp.ui.components.glassEffect
import com.example.cryptopredictionapp.ui.screens.AutoTraderScreen
import com.example.cryptopredictionapp.ui.screens.HomeScreen
import com.example.cryptopredictionapp.ui.theme.*
import com.example.cryptopredictionapp.ui.viewmodel.AutoTraderViewModel
import com.example.cryptopredictionapp.ui.viewmodel.CryptoViewModel

@Composable
fun MainScreen() {
    val cryptoViewModel: CryptoViewModel = viewModel()
    val autoTraderViewModel: AutoTraderViewModel = viewModel()
    var currentScreen by remember { mutableStateOf("HOME") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // --- 1. KATMAN: İÇERİK ---
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (currentScreen) {
                "HOME" -> HomeScreen(viewModel = cryptoViewModel)
                "SCANNER" -> AutoTraderScreen(
                    viewModel = autoTraderViewModel,
                    onInspectClick = { symbol ->
                        cryptoViewModel.onCoinSelected(symbol)
                        currentScreen = "HOME"
                    }
                )
            }
        }

        // --- 2. KATMAN: FLOATING GLASS NAVBAR (EN ÜSTTE) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = 24.dp)
                .height(70.dp)
                // --- ÇÖZÜM: HAYALET TIKLAMA KALKANI ---
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Tıklamayı yut */ }
                )
                // --- CAM EFEKTİ (Daha şeffaf, çünkü ikonların kendi arkası var artık) ---
                .glassEffect(cornerRadius = 50.dp, opacity = 0.05f)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavIcon(Icons.Default.Home, "Analiz", currentScreen == "HOME", NeonMagenta) { currentScreen = "HOME" }
                NavIcon(Icons.Default.Search, "Avcı", currentScreen == "SCANNER", AcidGreen) { currentScreen = "SCANNER" }
            }
        }
    }
}

@Composable
fun NavIcon(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(4.dp) // Dış boşluk
    ) {
        // --- İKON ARKASINDAKİ SİYAH GÖLGE DAİRESİ ---
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp) // Arkadaki siyah dairenin boyutu
                .clip(CircleShape)
                // İŞTE BURASI: %60 Siyah Opaklık. Arkadan renkli buton geçse de ikon görünür.
                .background(Color.Black.copy(alpha = 0.25f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                // Seçiliyse Neon rengi, değilse Gri/Beyaz
                tint = if (isSelected) selectedColor else Color.LightGray,
                modifier = Modifier.size(24.dp)
            )
        }

        if (isSelected) {
            // Seçili olduğunu gösteren minik nokta
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(selectedColor, CircleShape)
            )
        }
    }
}