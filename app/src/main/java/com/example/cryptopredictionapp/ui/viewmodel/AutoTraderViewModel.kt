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

    // Fırsatlar
    private val _opportunities = MutableStateFlow<List<TradeOpportunity>>(emptyList())
    val opportunities: StateFlow<List<TradeOpportunity>> = _opportunities.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
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
                    // Limit artırıldı (repository içinde 20 olarak ayarlı)
                    val result = repository.fetchTopMovers(limit = 20)
                    if (result.isNotEmpty()) {
                        _gainers.value = result["GAINERS"] ?: emptyList()
                        _losers.value = result["LOSERS"] ?: emptyList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(3000)
            }
        }
    }

    // --- 2. ANALİZ TETİKLEYİCİ (20 Fırsat Tarama) ---
    fun analyzeOpportunities() {
        if (_gainers.value.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Fırsatlar Taranıyor..."

            val foundOpps = mutableListOf<TradeOpportunity>()
            // Daha geniş bir havuzdan seçim yap (İlk 10 Gainer + İlk 10 Loser)
            val candidates = _gainers.value.take(10) + _losers.value.take(10)

            var processedCount = 0
            for (coin in candidates) {
                processedCount++
                _statusMessage.value = "Analiz: ${coin.symbol} ($processedCount/20)..."

                try {
                    // Hacim Filtresi
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
                    delay(100) // API yükünü azaltmak için
                } catch (e: Exception) { e.printStackTrace() }
            }

            _opportunities.value = foundOpps
            _statusMessage.value = if (foundOpps.isNotEmpty()) "✅ ${foundOpps.size} Fırsat Bulundu" else "Fırsat Bulunamadı"
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
        val cleanText = text.split(" ")[0].replace(",", ".")
        return cleanText.toDoubleOrNull() ?: 0.0
    }

    override fun onCleared() {
        super.onCleared()
        liveDataJob?.cancel()
    }

    // --- İŞLEM TETİKLEYİCİ ---
    private val _tradeResult = MutableStateFlow<String?>(null)
    val tradeResult: StateFlow<String?> = _tradeResult.asStateFlow()

    fun executeTrade(opp: TradeOpportunity) {
        viewModelScope.launch {
            _tradeResult.value = "Kasa kontrol ediliyor ve işlem hesaplanıyor..."
            val side = if (opp.type == "LONG") "BUY" else "SELL"

            // GÜNCELLENMİŞ FONKSİYON (TP/SL DAHİL)
            val result = repository.placeSmartTrade(
                symbol = opp.symbol,
                side = side,
                price = opp.entryPrice,
                leverage = 20, // Otomatik avcıda varsayılan 20x
                tpPrice = opp.takeProfit,
                slPrice = opp.stopLoss
            )
            _tradeResult.value = result

            delay(5000)
            _tradeResult.value = null
        }
    }
}