package com.example.cryptopredictionapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cryptopredictionapp.data.model.BingxMarketItem
import com.example.cryptopredictionapp.ui.components.glassEffect
import com.example.cryptopredictionapp.ui.theme.*
import com.example.cryptopredictionapp.ui.viewmodel.AutoTraderViewModel
import com.example.cryptopredictionapp.ui.viewmodel.TradeOpportunity
import java.util.Locale

@Composable
fun AutoTraderScreen(
    viewModel: AutoTraderViewModel = viewModel(),
    onInspectClick: (String) -> Unit
) {
    val gainers by viewModel.gainers.collectAsState()
    val losers by viewModel.losers.collectAsState()
    val opportunities by viewModel.opportunities.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var selectedTab by remember { mutableStateOf("GAINERS") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. HEADER ---
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Market Scanner",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite
            )
            // Canlı Rozeti (Sağda)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .glassEffect(cornerRadius = 50.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("●", color = AcidGreen, fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LIVE", color = AcidGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Durum Mesajı
        Text(statusMessage, fontSize = 11.sp, color = TextGray, modifier = Modifier.align(Alignment.Start))

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. TARA BUTONU (Geniş Neon Buton) ---
        Button(
            onClick = { viewModel.analyzeOpportunities() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = TextWhite)
            } else {
                Text("🔍 Fırsatları Tara (AI)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 3. FIRSAT LİSTESİ (Varsa) ---
        AnimatedVisibility(visible = opportunities.isNotEmpty()) {
            Column {
                Text("🔥 SIGNAL DETECTED", fontWeight = FontWeight.Bold, color = AcidGreen, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(opportunities) { opp ->
                        SignalCard(opp, onInspectClick = { onInspectClick(opp.symbol) })
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // --- 4. TABLO SEÇİCİ (Segmented Control) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .glassEffect(cornerRadius = 12.dp, opacity = 0.1f)
                .padding(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Gainers Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == "GAINERS") AcidGreen else Color.Transparent)
                        .clickable { selectedTab = "GAINERS" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Top Gainers",
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == "GAINERS") Color.Black else TextGray
                    )
                }
                // Losers Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == "LOSERS") NeonMagenta else Color.Transparent)
                        .clickable { selectedTab = "LOSERS" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Top Losers",
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == "LOSERS") TextWhite else TextGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 5. COIN LİSTESİ (Glass Strips) ---
        val currentList = if (selectedTab == "GAINERS") gainers else losers

        if (currentList.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Veri bekleniyor...", color = TextGray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(currentList) { item ->
                    MarketItemRow(item)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // Bottom Nav boşluğu
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

// --- ALT BİLEŞENLER ---

@Composable
fun SignalCard(opp: TradeOpportunity, onInspectClick: () -> Unit) {
    val isLong = opp.type == "LONG"
    val accentColor = if (isLong) AcidGreen else NeonMagenta
    val labelColor = if (isLong) Color.Black else TextWhite

    Box(modifier = Modifier.fillMaxWidth().glassEffect()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Küçük bir ikon kutusu
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(opp.symbol.take(1), fontWeight = FontWeight.Bold, color = accentColor)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(opp.symbol, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextWhite)
                }
                // Sinyal Tipi Rozeti
                Box(
                    modifier = Modifier
                        .background(accentColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(opp.type, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = labelColor)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = GlassWhite.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            // Fiyatlar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SignalInfoAuto("ENTRY", opp.entryPrice, TextWhite)
                SignalInfoAuto("TP", opp.takeProfit, AcidGreen)
                SignalInfoAuto("SL", opp.stopLoss, NeonMagenta)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Aksiyon Butonu
            Button(
                onClick = onInspectClick,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("GRAFİĞE GİT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
        }
    }
}

@Composable
fun SignalInfoAuto(label: String, price: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
        val formatted = if (price < 1.0) String.format(Locale.US, "%.5f", price) else String.format(Locale.US, "%.2f", price)
        Text(formatted, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun MarketItemRow(item: BingxMarketItem) {
    val priceChange = item.priceChangePercent?.toDoubleOrNull() ?: 0.0
    val isPositive = priceChange >= 0
    val changeColor = if (isPositive) AcidGreen else NeonMagenta

    // Her satır ince bir cam şerit
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(cornerRadius = 12.dp, opacity = 0.05f) // Çok şeffaf
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol Taraf: Sembol ve Hacim
            Column {
                Text(item.symbol.replace("-USDT", ""), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextWhite)
                Text("Vol: ${formatVolumeCompact(item.quoteVolume)}", fontSize = 12.sp, color = TextGray)
            }
            // Sağ Taraf: Fiyat ve Yüzde
            Column(horizontalAlignment = Alignment.End) {
                Text(item.lastPrice ?: "...", fontWeight = FontWeight.Medium, fontSize = 16.sp, color = TextWhite)
                Text(
                    text = "${if (isPositive) "+" else ""}${String.format("%.2f", priceChange)}%",
                    color = changeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

fun formatVolumeCompact(vol: String?): String {
    val v = vol?.toDoubleOrNull() ?: 0.0
    return when {
        v >= 1_000_000 -> String.format("%.1fM", v / 1_000_000)
        v >= 1_000 -> String.format("%.1fK", v / 1_000)
        else -> String.format("%.0f", v)
    }
}