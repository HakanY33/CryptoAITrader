package com.example.cryptopredictionapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.cryptopredictionapp.ui.components.CryptoChart
// DİKKAT: GlassComponents.kt dosyası ui/components altında olmalı
import com.example.cryptopredictionapp.ui.components.glassEffect
import com.example.cryptopredictionapp.ui.theme.*
import com.example.cryptopredictionapp.ui.viewmodel.CryptoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: CryptoViewModel) {
    // --- STATE TANIMLAMALARI ---
    val analysis by viewModel.analysisState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val tradeResult by viewModel.tradeResult.collectAsState()
    val userLeverage by viewModel.userLeverage.collectAsState()

    val searchText by viewModel.searchText.collectAsState()
    val filteredCoins by viewModel.filteredCoins.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val selectedSymbol by viewModel.selectedSymbol.collectAsState()
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsState()

    val context = LocalContext.current

    // İşlem Sonucu Bildirimi (Toast)
    LaunchedEffect(tradeResult) {
        tradeResult?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(selectedSymbol) {
        if (analysis.trend == "Analiz Bekleniyor..." || analysis.trend == "Bekleniyor...") {
            viewModel.analyzeMarket(selectedSymbol)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp), // Kenar boşlukları
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // --- 1. HEADER (Sadece Yazı) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "AI Crypto Analyzer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite
            )
        }

        // --- 2. GLASS SEARCH BAR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .glassEffect(cornerRadius = 16.dp) // Cam Efekti
                .zIndex(10f) // Dropdown üstte kalsın diye
        ) {
            TextField(
                value = searchText,
                onValueChange = { viewModel.onSearchTextChange(it) },
                placeholder = { Text("Coin Ara (Ör: ETH)", color = TextGray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = AcidGreen,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                singleLine = true,
                trailingIcon = {
                    if (isSearching) {
                        IconButton(onClick = { viewModel.onSearchTextChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Temizle", tint = TextWhite)
                        }
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "Ara", tint = AcidGreen)
                    }
                }
            )
        }

        // DROPDOWN LİSTE (Arama Sonuçları)
        if (isSearching && filteredCoins.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .zIndex(20f)
                    .glassEffect(cornerRadius = 12.dp, opacity = 0.9f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    filteredCoins.take(5).forEach { coin ->
                        Text(
                            text = coin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onCoinSelected(coin) }
                                .padding(12.dp),
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Divider(color = GlassWhite.copy(alpha = 0.2f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. SCROLLABLE CONTENT ---
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- ZAMAN SEÇİCİ ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(cornerRadius = 50.dp, opacity = 0.05f)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val timeframes = listOf("1m", "5m", "15m", "1h", "4h")
                timeframes.forEach { tf ->
                    val isSelected = selectedTimeframe == tf
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) AcidGreen else Color.Transparent)
                            .clickable { viewModel.onTimeframeSelected(tf) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tf,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else TextGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- CHART AREA ---
            if (analysis.candles.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .glassEffect()
                        .padding(8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedSymbol,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "$${analysis.currentPrice}",
                                color = AcidGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        CryptoChart(candles = analysis.candles, analysisState = analysis)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- SNIPER SETUP (GÜNCELLENMİŞ: 3 EŞİT SÜTUN) ---
            if (analysis.tradeTp.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().glassEffect()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🎯 SNIPER SETUP",
                            color = ElectricPurple,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // DÜZELTME: Row içindeki elemanlara 'weight(1f)' vererek eşit böldük
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            // 1. Sütun: GİRİŞ (Uzun yazı aşağı kayacak)
                            InfoColumn(
                                title = "Giriş",
                                value = analysis.tradeEntry,
                                color = TextWhite,
                                modifier = Modifier.weight(1f) // %33 Alan
                            )

                            // 2. Sütun: TP
                            InfoColumn(
                                title = "Hedef (TP)",
                                value = analysis.tradeTp,
                                color = AcidGreen,
                                modifier = Modifier.weight(1f) // %33 Alan
                            )

                            // 3. Sütun: SL
                            InfoColumn(
                                title = "Stop (SL)",
                                value = analysis.tradeSl,
                                color = NeonMagenta,
                                modifier = Modifier.weight(1f) // %33 Alan
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- AI INTELLIGENCE ---
            if (analysis.aiComment.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassEffect()
                        .border(1.dp, Brush.horizontalGradient(listOf(ElectricPurple, Color.Transparent)), RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🤖 Gemini AI", fontWeight = FontWeight.Bold, color = ElectricPurple)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("", color = AcidGreen, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            analysis.aiComment,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhite.copy(alpha = 0.9f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Button(
                    onClick = { viewModel.askAiCurrentState(selectedSymbol) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = TextWhite, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Yapay Zekaya Sor ✨", fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- ACTION CONTROL CENTER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect()
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kaldıraç", color = TextGray, fontSize = 12.sp)
                        Text("${userLeverage.toInt()}x", color = AcidGreen, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = userLeverage,
                        onValueChange = { viewModel.onLeverageChanged(it) },
                        valueRange = 1f..125f,
                        colors = SliderDefaults.colors(
                            thumbColor = AcidGreen,
                            activeTrackColor = AcidGreen,
                            inactiveTrackColor = GlassWhite.copy(alpha = 0.2f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.executeMarketTrade("BUY", analysis.tradeTp, analysis.tradeSl) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AcidGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("LONG 🚀", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                        }

                        Button(
                            onClick = { viewModel.executeMarketTrade("SELL", analysis.tradeTp, analysis.tradeSl) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("SHORT 🩸", color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text(
                            text = "Risk: Kasa %1 (Otomatik) * Kaldıraç Miktarı",
                            color = TextGray,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// --- GÜNCELLENMİŞ YARDIMCI BİLEŞEN: InfoColumn ---
@Composable
fun InfoColumn(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier // Modifier parametresi eklendi
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = title,
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Değer (Uzun metinler için ayarlar)
        Text(
            text = value,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp, // Puntosunu hafif küçülttük
            textAlign = TextAlign.Center, // Ortala
            lineHeight = 18.sp // Satır arası boşluk
        )
    }
}