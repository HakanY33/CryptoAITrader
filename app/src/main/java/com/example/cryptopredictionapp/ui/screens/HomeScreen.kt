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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.cryptopredictionapp.ui.components.CryptoChart
import com.example.cryptopredictionapp.ui.components.glassEffect // Yeni GlassModifier'ımız
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
                color = TextWhite // Color.kt'den geliyor
            )
        }

        // --- 2. GLASS SEARCH BAR ---
        // Standart OutlinedTextField yerine GlassBox içine TextField koyuyoruz
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
                    .background(Color.Transparent), // Arkaplan şeffaf, cam görünsün
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, // Çizgileri kaldır
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
                    .zIndex(20f) // Search bar'ın da üstünde
                    .glassEffect(cornerRadius = 12.dp, opacity = 0.9f) // Daha opak cam
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    filteredCoins.take(5).forEach { coin -> // Max 5 sonuç gösterelim
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
            // --- ZAMAN SEÇİCİ (Segmented Look) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(cornerRadius = 50.dp, opacity = 0.05f) // İnce bir bar
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

            // --- CHART AREA (Glass Card) ---
            if (analysis.candles.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .glassEffect() // SİHİRLİ DOKUNUŞ
                        .padding(8.dp)
                ) {
                    Column {
                        // Chart Başlığı
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
                        // Grafik Bileşeni
                        CryptoChart(candles = analysis.candles, analysisState = analysis)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- SNIPER SETUP (Wide Glass Card) ---
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Entry (Beyaz)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Giriş", color = TextGray, fontSize = 11.sp)
                                Text(analysis.tradeEntry, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            // TP (Yeşil)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Hedef (TP)", color = TextGray, fontSize = 11.sp)
                                Text(analysis.tradeTp, color = AcidGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            // SL (Magenta/Kırmızı)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Stop (SL)", color = TextGray, fontSize = 11.sp)
                                Text(analysis.tradeSl, color = NeonMagenta, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- AI INTELLIGENCE (Glowing Border) ---
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
                            // Güven Skoru (Confidence) yok (Şimdilik)
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
                // AI Bekleniyor Butonu (Eğer yorum yoksa)
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

            // --- ACTION CONTROL CENTER (Leverage + Buttons) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect()
                    .padding(16.dp)
            ) {
                Column {
                    // Kaldıraç Slider
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

                    // Butonlar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // LONG Button
                        Button(
                            onClick = { viewModel.executeMarketTrade("BUY", analysis.tradeTp, analysis.tradeSl) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AcidGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("LONG 🚀", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                        }

                        // SHORT Button
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

                    // Risk Bilgisi
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text(
                            text = "Risk: Kasa %1 (Otomatik) * Kaldıraç Miktarı",
                            color = TextGray,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Bottom Nav için boşluk
        }
    }
}