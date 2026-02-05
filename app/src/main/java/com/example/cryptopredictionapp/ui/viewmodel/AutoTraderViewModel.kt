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

    // Piyasa Listeleri
    private val _gainers = MutableStateFlow<List<BingxMarketItem>>(emptyList())
    val gainers: StateFlow<List<BingxMarketItem>> = _gainers.asStateFlow()

    private val _losers = MutableStateFlow<List<BingxMarketItem>>(emptyList())
    val losers: StateFlow<List<BingxMarketItem>> = _losers.asStateFlow()

    private val _opportunities = MutableStateFlow<List<TradeOpportunity>>(emptyList())
    val opportunities: StateFlow<List<TradeOpportunity>> = _opportunities.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow("Piyasa Bekleniyor...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var liveDataJob: Job? = null

    init {
        startLiveMarketData()
    }

    private fun startLiveMarketData() {
        liveDataJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val result = repository.fetchTopMovers(limit = 20)
                    if (result.isNotEmpty()) {
                        _gainers.value = result["GAINERS"] ?: emptyList()
                        _losers.value = result["LOSERS"] ?: emptyList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000) // 5 saniyede bir yenile (API kotasını koru)
            }
        }
    }

    fun analyzeOpportunities() {
        // Liste boşsa önce veri çekmeyi dene
        if (_gainers.value.isEmpty()) {
            _statusMessage.value = "Veri bekleniyor..."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _opportunities.value = emptyList() // Listeyi temizle

            val foundOpps = mutableListOf<TradeOpportunity>()
            // En çok yükselen ve düşenlerden karma bir havuz oluştur
            val candidates = _gainers.value.take(15) + _losers.value.take(15)

            var processedCount = 0
            for (coin in candidates) {
                processedCount++
                _statusMessage.value = "Analiz: ${coin.symbol} ($processedCount/${candidates.size})"

                try {
                    // Hacim Filtresi (Düşük hacimli coinlerden uzak dur)
                    val volume = coin.quoteVolume?.toDoubleOrNull() ?: 0.0
                    if (volume < 1_000_000) continue

                    // Mum Verilerini Çek
                    val candles = repository.getKlinesData(coin.symbol, "15m")
                    if (candles.size > 50) {

                        val price = coin.lastPrice?.toDoubleOrNull() ?: 0.0

                        // İndikatör Hazırlığı
                        val candlesReversed = candles.reversed()
                        val closes = candlesReversed.map { BigDecimal(it.close) }
                        val highs = candlesReversed.map { BigDecimal(it.high) }
                        val lows = candlesReversed.map { BigDecimal(it.low) }

                        // ATR Hesapla
                        val atr = IndicatorUtils.calculateATR(highs, lows, closes)

                        if (atr != null && price > 0) {
                            // --- YENİ HYBRID MOTOR ---
                            val setup = TechnicalAnalysis.calculateHybridTradeSetup(
                                candles = candles,
                                currentPrice = BigDecimal(price),
                                atr = atr
                            )

                            val entryText = setup.first

                            // Geçerli bir işlem bulduysa
                            if (!entryText.contains("Bekle") &&
                                !entryText.contains("Yetersiz") &&
                                !entryText.contains("İşlem Yok")) {

                                // String içinden fiyatları temizle
                                val cleanEntry = entryText.split(" ").firstOrNull()?.replace(",", ".")?.toDoubleOrNull() ?: price
                                val cleanTp = setup.second.replace(",", ".").toDoubleOrNull() ?: 0.0
                                val cleanSl = setup.third.replace(",", ".").toDoubleOrNull() ?: 0.0

                                // TP ve SL geçerliyse ekle
                                if (cleanTp > 0 && cleanSl > 0) {
                                    val signalType = if (cleanTp > cleanEntry) "LONG" else "SHORT"

                                    foundOpps.add(
                                        TradeOpportunity(
                                            symbol = coin.symbol,
                                            type = signalType,
                                            entryPrice = cleanEntry,
                                            takeProfit = cleanTp,
                                            stopLoss = cleanSl,
                                            score = 85
                                        )
                                    )
                                }
                            }
                        }
                    }
                    delay(150) // API Spam koruması
                } catch (e: Exception) {
                    println("Hata (${coin.symbol}): ${e.message}")
                }
            }

            _opportunities.value = foundOpps
            _statusMessage.value = if (foundOpps.isNotEmpty()) "✅ ${foundOpps.size} Fırsat Bulundu" else "Fırsat Yok, Bekleniyor..."
            _isLoading.value = false
        }
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
            _tradeResult.value = "İşlem iletiliyor..."
            val side = if (opp.type == "LONG") "BUY" else "SELL"

            val result = repository.placeSmartTrade(
                symbol = opp.symbol,
                side = side,
                price = opp.entryPrice,
                leverage = 20,
                tpPrice = opp.takeProfit,
                slPrice = opp.stopLoss
            )
            _tradeResult.value = result

            delay(4000)
            _tradeResult.value = null
        }
    }
}