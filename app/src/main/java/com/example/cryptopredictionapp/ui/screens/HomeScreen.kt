package com.example.cryptopredictionapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.cryptopredictionapp.ui.viewmodel.CryptoViewModel
import com.example.cryptopredictionapp.ui.components.CryptoChart

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

    // Eğer Avcı ekranından geldiysek ve analiz henüz yapılmadıysa, otomatik başlat
    LaunchedEffect(selectedSymbol) {
        if (analysis.trend == "Analiz Bekleniyor..." || analysis.trend == "Bekleniyor...") {
            viewModel.analyzeMarket(selectedSymbol)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. SABİT ALAN: BAŞLIK VE ARAMA ---
        Text(
            text = "AI ANALİZ TERMİNALİ",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ARAMA ÇUBUĞU
        OutlinedTextField(
            value = searchText,
            onValueChange = { viewModel.onSearchTextChange(it) },
            label = { Text("Coin Ara (Ör: ETH)") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) viewModel.onSearchFocus()
                },
            singleLine = true,
            trailingIcon = {
                if (isSearching) {
                    IconButton(onClick = { viewModel.onSearchTextChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Temizle")
                    }
                } else {
                    Icon(Icons.Default.Search, contentDescription = "Ara")
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray
            )
        )

        // AÇILIR LİSTE (Dropdown - ZIndex ile en üstte)
        if (isSearching && filteredCoins.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .background(Color.White)
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .padding(bottom = 8.dp)
                    .zIndex(10f)
            ) {
                items(filteredCoins) { coin ->
                    Text(
                        text = coin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onCoinSelected(coin) }
                            .padding(16.dp),
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Divider(color = Color.LightGray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. KAYDIRILABİLİR İÇERİK ALANI ---
        Column(
            modifier = Modifier
                .weight(1f) // Ekranın geri kalanını kapla
                .verticalScroll(rememberScrollState()), // Kaydırılabilir yap
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- ZAMAN SEÇİCİ ---
            val timeframes = listOf("1m", "5m", "15m", "30m", "1h", "4h")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                timeframes.forEach { tf ->
                    val isSelected = selectedTimeframe == tf
                    Button(
                        onClick = { viewModel.onTimeframeSelected(tf) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF673AB7) else Color.LightGray,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .width(50.dp)
                    ) {
                        Text(text = tf, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- GRAFİK ALANI ---
            if (analysis.candles.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "${selectedSymbol} (${selectedTimeframe})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                        )
                        CryptoChart(candles = analysis.candles, analysisState = analysis)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- BİLGİ KARTI ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Yapay Zeka Analiz Ediyor...", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        Text(selectedSymbol, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.Black)
                        Text("Fiyat: $${analysis.currentPrice}", fontSize = 20.sp, color = Color.Black)

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("EMA 21: ${analysis.ema21}", color = Color.Black, fontSize = 12.sp)
                            Text("EMA 50: ${analysis.ema50}", color = Color.Black, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("SMC Analizi:", fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("OB: ${analysis.obStatus}", fontSize = 13.sp, color = Color.DarkGray)
                        Text("FVG: ${analysis.fvgStatus}", fontSize = 13.sp, color = Color.DarkGray)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = analysis.trend,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (analysis.trend.contains("YÜKSELİŞ") || analysis.trend.contains("GÜÇLÜ")) Color(0xFF00C853) else Color(0xFFD32F2F)
                        )

                        Text(
                            text = analysis.recommendation,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 8.dp),
                            color = Color.Black,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- TRADE SETUP KARTI ---
            if (analysis.tradeTp.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🎯 SNIPER SETUP", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), modifier = Modifier.align(Alignment.CenterHorizontally))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            InfoColumn("Giriş", analysis.tradeEntry, Color.Black)
                            InfoColumn("TP", analysis.tradeTp, Color(0xFF00C853))
                            InfoColumn("SL", analysis.tradeSl, Color(0xFFD32F2F))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- BUTONLAR (ANALİZ ve AI) ---
            Button(
                onClick = { viewModel.analyzeMarket(selectedSymbol) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("Manuel Analiz (Yenile)")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.askAiCurrentState(selectedSymbol) },
                enabled = analysis.trend != "Bekleniyor...",
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
            ) {
                Text("🤖 Yapay Zeka ile Yorumla")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- AI YORUMU ---
            if (analysis.aiComment.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🤖 Gemini Yorumu:", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(analysis.aiComment, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- HIZLI İŞLEM PANELİ (ARTIK SCROLLABLE ALANIN EN ALTINDA) ---
            // Burası eskiden sabitti, şimdi akışın bir parçası.
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⚡ Hızlı İşlem Paneli", fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Kaldıraç Slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Kaldıraç: ${userLeverage.toInt()}x", fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp), color = Color.Black)
                        Slider(
                            value = userLeverage,
                            onValueChange = { viewModel.onLeverageChanged(it) },
                            valueRange = 1f..125f,
                            steps = 124,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF673AB7), activeTrackColor = Color(0xFF673AB7)),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // AL - SAT Butonları
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.executeMarketTrade("BUY", analysis.tradeTp, analysis.tradeSl) },
                            modifier = Modifier.weight(1f).height(45.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("LONG 🚀", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.executeMarketTrade("SELL", analysis.tradeTp, analysis.tradeSl) },
                            modifier = Modifier.weight(1f).height(45.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SHORT 🩸", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Risk: Kasa %1 (Otomatik Hesaplanır)",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            // En alta biraz boşluk bırakalım ki navigation bar ile çakışmasın
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// --- YARDIMCI BİLEŞEN: InfoColumn (Dosyanın EN ALTINDA) ---
@Composable
fun InfoColumn(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}