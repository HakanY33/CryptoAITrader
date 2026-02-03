package com.example.cryptopredictionapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cryptopredictionapp.data.model.BingxMarketItem
import com.example.cryptopredictionapp.data.repository.CryptoRepository
import com.example.cryptopredictionapp.util.IndicatorUtils
import com.example.cryptopredictionapp.util.TechnicalAnalysis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class TradeOpportunity(
    val symbol: String,
    val type: String,
    val entryPrice: Double,
    val takeProfit: Double,
    val stopLoss: Double,
    val score: Int
)

class AutoTraderViewModel : ViewModel() {

    private val repository = CryptoRepository()

    // Piyasa Listeleri (Sürekli Güncel)
    private val _gainers = MutableStateFlow<List<BingxMarketItem>>(emptyList())
    val gainers: StateFlow<List<BingxMarketItem>> = _gainers.asStateFlow()

    private val _losers = MutableStateFlow<List<BingxMarketItem>>(emptyList())
    val losers: StateFlow<List<BingxMarketItem>> = _losers.asStateFlow()

    // Fırsatlar (Sadece Butona Basınca Güncellenir)
    private val _opportunities = MutableStateFlow<List<TradeOpportunity>>(emptyList())
    val opportunities: StateFlow<List<TradeOpportunity>> = _opportunities.asStateFlow()

    private val _isLoading = MutableStateFlow(false) // Sadece Analiz sırasında döner
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow("Piyasa Canlı Takip Ediliyor...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var liveDataJob: Job? = null

    init {
        startLiveMarketData()
    }

    // --- 1. CANLI VERİ AKIŞI (Arka Planda Sürekli Çalışır) ---
    private fun startLiveMarketData() {
        liveDataJob = viewModelScope.launch {
            while (isActive) {
                try {
                    // Sessizce veriyi çekip listeyi güncelle
                    val result = repository.fetchTopMovers()
                    if (result.isNotEmpty()) {
                        _gainers.value = result["GAINERS"] ?: emptyList()
                        _losers.value = result["LOSERS"] ?: emptyList()
                    }
                } catch (e: Exception) {
                    // Sessiz hata (Kullanıcıyı rahatsız etme, bir sonraki turda düzelir)
                    e.printStackTrace()
                }
                // 3 Saniye bekle (Anlık hissi vermek için ideal)
                delay(3000)
            }
        }
    }

    // --- 2. ANALİZ TETİKLEYİCİ (Butonla Çalışır) ---
    fun analyzeOpportunities() {
        if (_gainers.value.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Fırsatlar Taranıyor..."

            val foundOpps = mutableListOf<TradeOpportunity>()
            // Anlık listeden ilk 5'i al
            val candidates = _gainers.value.take(5) + _losers.value.take(5)

            var processedCount = 0
            for (coin in candidates) {
                processedCount++
                _statusMessage.value = "Analiz: ${coin.symbol}..."

                try {
                    // Hacim Filtresi (>500k)
                    val volume = coin.quoteVolume?.toDoubleOrNull() ?: 0.0
                    if (volume < 500_000) continue

                    // Detaylı Mum Analizi
                    val candles = repository.getKlinesData(coin.symbol, "15m")
                    if (candles.size > 50) {
                        val score = calculateScore(candles)

                        if (score >= 2) {
                            val price = coin.lastPrice?.toDoubleOrNull() ?: 0.0
                            val isShort = coin.priceChangePercent?.contains("-") == true
                            val type = if (isShort) "SHORT" else "LONG"

                            val obText = TechnicalAnalysis.findOrderBlock(candles)
                            val fvgText = TechnicalAnalysis.findFVG(candles)

                            val closes = candles.map { BigDecimal(it.close) }
                            val highs = candles.map { BigDecimal(it.high) }
                            val lows = candles.map { BigDecimal(it.low) }
                            val atr = IndicatorUtils.calculateATR(highs, lows, closes)

                            var finalEntry = price
                            var finalTp = price * 1.02
                            var finalSl = price * 0.99

                            if (atr != null) {
                                val setup = TechnicalAnalysis.calculateSmartTradeSetup(
                                    BigDecimal(price), atr, type, obText, fvgText
                                )
                                finalEntry = parsePrice(setup.first)
                                finalTp = parsePrice(setup.second)
                                finalSl = parsePrice(setup.third)
                            }

                            if (price > 0) {
                                foundOpps.add(TradeOpportunity(coin.symbol, type, finalEntry, finalTp, finalSl, score))
                            }
                        }
                    }
                    delay(100) // API'yi boğmamak için minik bekleme
                } catch (e: Exception) { e.printStackTrace() }
            }

            _opportunities.value = foundOpps
            _statusMessage.value = if (foundOpps.isNotEmpty()) "✅ ${foundOpps.size} Fırsat Bulundu (Liste Canlı)" else "Fırsat Bulunamadı (Liste Canlı)"
            _isLoading.value = false
        }
    }

    private fun calculateScore(candles: List<com.example.cryptopredictionapp.data.model.BingxKlineData>): Int {
        var score = 0
        val closes = candles.map { BigDecimal(it.close) }
        val ema21 = IndicatorUtils.calculateEMA(closes, 21)
        val ema50 = IndicatorUtils.calculateEMA(closes, 50)
        if (ema21 != null && ema50 != null) {
            if (ema21 > ema50) score++
            val rsi = IndicatorUtils.calculateRSI(closes)
            if (rsi != null && rsi > BigDecimal(50)) score++
        }
        return score
    }

    private fun parsePrice(text: String): Double {
        val cleanText = text.split(" ")[0]
        return cleanText.toDoubleOrNull() ?: 0.0
    }

    override fun onCleared() {
        super.onCleared()
        liveDataJob?.cancel()
    }
}