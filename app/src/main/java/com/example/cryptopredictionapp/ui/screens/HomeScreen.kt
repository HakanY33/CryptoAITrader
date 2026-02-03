package com.example.cryptopredictionapp.ui.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cryptopredictionapp.ui.viewmodel.CryptoViewModel
import com.example.cryptopredictionapp.ui.components.CryptoChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: CryptoViewModel = viewModel()) {
    val analysis by viewModel.analysisState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val searchText by viewModel.searchText.collectAsState()
    val filteredCoins by viewModel.filteredCoins.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val selectedSymbol by viewModel.selectedSymbol.collectAsState()
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. SABİT ALAN: BAŞLIK VE ARAMA ---
        Text(
            text = "AI Crypto Analyzer",
            style = MaterialTheme.typography.headlineMedium,
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

        // AÇILIR LİSTE (Dropdown)
        if (isSearching && filteredCoins.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp) // Maksimum yükseklik
                    .background(Color.White)
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .padding(bottom = 8.dp)
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

        // --- 2. KAYDIRILABİLİR ALAN ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- YENİ: ZAMAN SEÇİCİ (TIMELINE) BUTONLARI ---
            // Scalp için 1m, 5m, 15m, 30m eklendi
            val timeframes = listOf("1m", "5m", "15m", "30m", "1h", "4h")

            // Butonları sığdırmak için 2 satıra bölebiliriz veya kaydırılabilir Row yapabiliriz.
            // Burda basitçe sığdırmaya çalışalım, sığmazsa Row'u horizontalScroll yaparsın.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                timeframes.forEach { tf ->
                    Button(
                        onClick = { viewModel.onTimeframeSelected(tf) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTimeframe == tf) Color(0xFF673AB7) else Color.DarkGray,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .height(35.dp)
                            .widthIn(min = 40.dp) // Butonlar çok geniş olmasın
                    ) {
                        Text(text = tf, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- GRAFİK ALANI ---
            if (analysis.candles.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp), // Grafik için yer
                    elevation = CardDefaults.cardElevation(8.dp),
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

            // BİLGİ KARTI
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Text(selectedSymbol, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.Black)
                        Text("Fiyat: $${analysis.currentPrice}", fontSize = 20.sp, color = Color.Black)

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // İndikatörler
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("EMA 21: ${analysis.ema21}", color = Color.Black)
                            Text("EMA 50: ${analysis.ema50}", color = Color.Black)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("SMC Analizi:", fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("OB: ${analysis.obStatus}", fontSize = 14.sp, color = Color.DarkGray)
                        Text("FVG: ${analysis.fvgStatus}", fontSize = 14.sp, color = Color.DarkGray)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = analysis.trend,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if(analysis.trend.contains("YÜKSELİŞ") || analysis.trend.contains("GÜÇLÜ")) Color(0xFF007E33) else Color(0xFFCC0000)
                        )

                        Text(
                            text = analysis.recommendation,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 8.dp),
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- TRADE SETUP KARTI (Sinyal Varsa) ---
            if (analysis.tradeTp.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🎯 SNIPER SETUP (${selectedTimeframe})",
                            color = Color.Yellow,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("GİRİŞ", color = Color.Gray, fontSize = 12.sp)
                                Text(analysis.tradeEntry, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("HEDEF (TP)", color = Color.Green, fontSize = 12.sp)
                                Text(analysis.tradeTp, color = Color.Green, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("STOP (SL)", color = Color.Red, fontSize = 12.sp)
                                Text(analysis.tradeSl, color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ANALİZ BUTONU (Artık state'teki timeframe'i kullanıyor)
            Button(
                onClick = { viewModel.analyzeMarket(selectedSymbol) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Analiz Yap ($selectedTimeframe)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI BUTONU
            Button(
                onClick = { viewModel.askAiCurrentState(selectedSymbol) },
                enabled = analysis.trend != "Bekleniyor...",
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF673AB7),
                    contentColor = Color.White
                )
            ) {
                Text("🤖 Yapay Zeka ile Yorumla")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI YORUMU
            if (analysis.aiComment.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🤖 Gemini Yorumu:",
                            fontWeight = FontWeight.Bold,
                            color = Color.Blue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = analysis.aiComment,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}