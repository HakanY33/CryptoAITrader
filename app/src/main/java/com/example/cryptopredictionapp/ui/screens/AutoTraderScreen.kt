package com.example.cryptopredictionapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cryptopredictionapp.data.model.BingxMarketItem
import com.example.cryptopredictionapp.ui.viewmodel.AutoTraderViewModel
import com.example.cryptopredictionapp.ui.viewmodel.TradeOpportunity
import java.util.Locale

@Composable
fun AutoTraderScreen(
    viewModel: AutoTraderViewModel = viewModel(),
    onInspectClick: (String) -> Unit // <--- YENİ: Ana ekrana geçiş fonksiyonu
) {
    val gainers by viewModel.gainers.collectAsState()
    val losers by viewModel.losers.collectAsState()
    val opportunities by viewModel.opportunities.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var selectedTab by remember { mutableStateOf("GAINERS") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- BAŞLIK ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ OTOMATİK AVCI",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF673AB7)
            )
            // Canlı Rozeti
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(50)
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("●", color = Color.Green, fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TARAMA AKTİF", color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(statusMessage, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))

        // --- TARA BUTONU ---
        Button(
            onClick = { viewModel.analyzeOpportunities() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Piyasa Taranıyor (%20)...")
            } else {
                Text("🔍 Fırsatları Bul (20 Coin)")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- FIRSAT LİSTESİ ---
        AnimatedVisibility(visible = opportunities.isNotEmpty()) {
            Column {
                Text("🔥 YAKALANAN FIRSATLAR", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(opportunities) { opp ->
                        // BURADA onInspectClick ÇAĞRILIYOR
                        SignalCard(opp, onInspectClick = { onInspectClick(opp.symbol) })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // --- PIYASA LİSTESİ (TABLO) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TabButton("🚀 Yükselenler", selectedTab == "GAINERS", Color(0xFF00C853)) { selectedTab = "GAINERS" }
            TabButton("🩸 Düşenler", selectedTab == "LOSERS", Color(0xFFD32F2F)) { selectedTab = "LOSERS" }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val currentList = if (selectedTab == "GAINERS") gainers else losers
        if (currentList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Veri bekleniyor...", color = Color.Gray)
            }
        } else {
            LazyColumn {
                items(currentList) { item ->
                    MarketItemRow(item)
                    Divider(color = Color.LightGray.copy(alpha = 0.3f))
                }
            }
        }
    }
}

// --- YARDIMCI BİLEŞENLER ---

@Composable
fun TabButton(text: String, isSelected: Boolean, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else Color.Transparent,
            contentColor = if (isSelected) Color.White else Color.Gray
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp),
        modifier = Modifier.height(40.dp)
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
fun SignalCard(opp: TradeOpportunity, onInspectClick: () -> Unit) {
    val isLong = opp.type == "LONG"
    val cardColor = if (isLong) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val textColor = if (isLong) Color(0xFF2E7D32) else Color(0xFFC62828)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(opp.symbol, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(opp.type, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = textColor)
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.3f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SignalInfoAuto("GİRİŞ", opp.entryPrice)
                SignalInfoAuto("TP", opp.takeProfit, Color(0xFF00C853))
                SignalInfoAuto("SL", opp.stopLoss, Color(0xFFD32F2F))
            }
            Spacer(modifier = Modifier.height(12.dp))

            // İNCELE BUTONU
            Button(
                onClick = onInspectClick,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("🔍 GRAFİĞİ İNCELE & İŞLEM AÇ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// İsim çakışması olmasın diye buna "Auto" son ekini ekledim
@Composable
fun SignalInfoAuto(label: String, price: Double, color: Color = Color.Black) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        // Fiyat gösterimi (RATS gibi coinler için hassas)
        val formatted = if (price < 1.0) String.format(Locale.US, "%.6f", price) else String.format(Locale.US, "%.2f", price)
        Text(formatted, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun MarketItemRow(item: BingxMarketItem) {
    val priceChange = item.priceChangePercent?.toDoubleOrNull() ?: 0.0
    val isPositive = priceChange >= 0
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(item.symbol.replace("-USDT", ""), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Vol: ${formatVolumeCompact(item.quoteVolume)}", fontSize = 12.sp, color = Color.Gray)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(item.lastPrice ?: "...", fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text("${if (isPositive) "+" else ""}${String.format("%.2f", priceChange)}%", color = if (isPositive) Color(0xFF00C853) else Color(0xFFD32F2F), fontWeight = FontWeight.Bold, fontSize = 14.sp)
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