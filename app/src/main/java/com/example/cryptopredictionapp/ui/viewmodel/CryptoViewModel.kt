package com.example.cryptopredictionapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cryptopredictionapp.data.model.MarketDataRequest
import com.example.cryptopredictionapp.data.repository.CryptoRepository
import com.example.cryptopredictionapp.util.IndicatorUtils
import com.example.cryptopredictionapp.util.TechnicalAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class AnalysisState(
    val currentPrice: String = "...",
    val ema21: String = "...",
    val ema50: String = "...",
    val obStatus: String = "...",
    val fvgStatus: String = "...",
    val trend: String = "Bekleniyor...",
    val recommendation: String = "Veri Yok",
    val aiComment: String = "",
    val strategyScore: String = "0/6",
    val tradeEntry: String = "",
    val tradeTp: String = "",
    val tradeSl: String = "",
    val candles: List<com.example.cryptopredictionapp.data.model.BingxKlineData> = emptyList()
)

class CryptoViewModel : ViewModel() {

    private val repository = CryptoRepository()

    // --- ARAMA DEĞİŞKENLERİ ---
    private var allCoins = listOf<String>()
    private val _filteredCoins = MutableStateFlow<List<String>>(emptyList())
    val filteredCoins: StateFlow<List<String>> = _filteredCoins.asStateFlow()
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    private val _selectedSymbol = MutableStateFlow("BTC-USDT")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    // --- YENİ: ZAMAN DİLİMİ (TIMEFRAME) ---
    private val _selectedTimeframe = MutableStateFlow("1h") // Varsayılan 1 Saat
    val selectedTimeframe: StateFlow<String> = _selectedTimeframe.asStateFlow()

    // --- ANALİZ DURUMU ---
    private val _analysisState = MutableStateFlow(AnalysisState())
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadCoinList()
        analyzeMarket("BTC-USDT")
    }

    private fun loadCoinList() {
        viewModelScope.launch {
            allCoins = repository.getAllSymbols()
            _filteredCoins.value = emptyList()
        }
    }

    fun onSearchTextChange(text: String) {
        _searchText.value = text
        _isSearching.value = true
        if (text.isEmpty()) {
            _filteredCoins.value = allCoins
        } else {
            _filteredCoins.value = allCoins.filter { it.contains(text, ignoreCase = true) }
        }
    }

    fun onSearchFocus() {
        _isSearching.value = true
        if (_searchText.value.isEmpty()) _filteredCoins.value = allCoins
    }

    fun onCoinSelected(symbol: String) {
        _selectedSymbol.value = symbol
        _searchText.value = symbol
        _isSearching.value = false
        analyzeMarket(symbol)
    }

    // --- YENİ: ZAMAN DEĞİŞTİRME FONKSİYONU ---
    fun onTimeframeSelected(interval: String) {
        _selectedTimeframe.value = interval
        // Zaman değişince otomatik olarak o anki coin için yeniden analiz yap
        analyzeMarket(_selectedSymbol.value)
    }

    // --- ANA ANALİZ FONKSİYONU ---
    // Not: interval parametresini kaldırdık, artık içeriden okuyor
    fun analyzeMarket(symbol: String) {
        val interval = _selectedTimeframe.value // Seçili zamanı al

        viewModelScope.launch {
            _isLoading.value = true
            _analysisState.value = AnalysisState(currentPrice = "Yükleniyor...")

            val rawCandles = repository.getKlinesData(symbol, interval)
            val currentTicker = repository.getCryptoPrice(symbol)

            if (rawCandles.isNotEmpty() && currentTicker != null) {
                val candlesReversed = rawCandles.reversed()
                val closes = candlesReversed.map { BigDecimal(it.close) }
                val highs = candlesReversed.map { BigDecimal(it.high) }
                val lows = candlesReversed.map { BigDecimal(it.low) }
                val volumes = candlesReversed.map { BigDecimal(it.volume) }

                // --- 6 STRATEJİ OYLAMASI ---
                var longVotes = 0
                var shortVotes = 0

                // 1. EMA
                val ema21 = IndicatorUtils.calculateEMA(closes, 21)
                val ema50 = IndicatorUtils.calculateEMA(closes, 50)
                if (ema21 != null && ema50 != null) {
                    if (ema21 > ema50) longVotes++ else shortVotes++
                }

                // 2. Alligator
                val alligator = IndicatorUtils.calculateAlligator(closes)
                if (alligator != null) {
                    val (jaw, teeth, lips) = alligator
                    if (lips > teeth && teeth > jaw) longVotes++
                    if (jaw > teeth && teeth > lips) shortVotes++
                }

                // 3. MFI + CMF
                val mfi = IndicatorUtils.calculateMFI(highs, lows, closes, volumes)
                val cmf = IndicatorUtils.calculateCMF(highs, lows, closes, volumes)
                if (mfi != null && cmf != null) {
                    if (mfi > BigDecimal(50) && cmf > BigDecimal(0.05)) longVotes++
                    if (mfi < BigDecimal(50) && cmf < BigDecimal(-0.05)) shortVotes++
                }

                // 4. Aroon
                val aroon = IndicatorUtils.calculateAroon(highs, lows)
                if (aroon != null) {
                    val (up, down) = aroon
                    if (up > BigDecimal(70) && up > down) longVotes++
                    if (down > BigDecimal(70) && down > up) shortVotes++
                }

                // 5. RSI
                val rsi = IndicatorUtils.calculateRSI(closes)
                if (rsi != null) {
                    if (rsi > BigDecimal(50)) longVotes++ else shortVotes++
                }

                // 6. ADX + OBV
                val adxData = IndicatorUtils.calculateADX(highs, lows, closes)
                val obvData = IndicatorUtils.calculateOBV(closes, volumes)
                if (adxData != null && obvData != null) {
                    val (adx, pDi, mDi) = adxData
                    val (obv, obvMa) = obvData
                    if (adx > BigDecimal(25)) {
                        if (pDi > mDi && obv > obvMa) longVotes++
                        if (mDi > pDi && obv < obvMa) shortVotes++
                    }
                }

                val obStatus = TechnicalAnalysis.findOrderBlock(rawCandles)
                val fvgStatus = TechnicalAnalysis.findFVG(rawCandles)

                var trendText = "YATAY / BELİRSİZ"
                var signalText = "İşlem Açma (Bekle)"
                val scoreDisplay = "Long: $longVotes / Short: $shortVotes"

                if (longVotes >= 4) {
                    trendText = "GÜÇLÜ YÜKSELİŞ 🚀"
                    signalText = "LONG İşlem Fırsatı (Skor: $longVotes/6)"
                } else if (shortVotes >= 4) {
                    trendText = "GÜÇLÜ DÜŞÜŞ 🩸"
                    signalText = "SHORT İşlem Fırsatı (Skor: $shortVotes/6)"
                }

                // --- TP / SL ---
                var entry = ""
                var tp = ""
                var sl = ""

                if (trendText.contains("GÜÇLÜ")) {
                    val atr = IndicatorUtils.calculateATR(highs, lows, closes)
                    if (atr != null) {
                        val currentBigDec = BigDecimal(currentTicker.lastPrice)
                        val setup = TechnicalAnalysis.calculateTradeSetup(currentBigDec, atr, trendText)
                        entry = setup.first
                        tp = setup.second
                        sl = setup.third
                    }
                }

                _analysisState.value = AnalysisState(
                    currentPrice = IndicatorUtils.formatPrice(BigDecimal(currentTicker.lastPrice)),
                    ema21 = IndicatorUtils.formatPrice(ema21),
                    ema50 = IndicatorUtils.formatPrice(ema50),
                    obStatus = obStatus,
                    fvgStatus = fvgStatus,
                    trend = trendText,
                    recommendation = signalText,
                    strategyScore = scoreDisplay,
                    aiComment = "",
                    tradeEntry = entry,
                    tradeTp = tp,
                    tradeSl = sl,
                    candles = rawCandles // Grafik için veri
                )
            } else {
                _analysisState.value = _analysisState.value.copy(recommendation = "Veri Alınamadı")
            }
            _isLoading.value = false
        }
    }

    fun askAiCurrentState(symbol: String) {
        val currentState = _analysisState.value
        if (currentState.trend == "Bekleniyor...") return

        viewModelScope.launch {
            _analysisState.value = currentState.copy(aiComment = "Yapay Zeka Stratejini İnceliyor... 🤖")
            val request = MarketDataRequest(
                symbol = symbol,
                price = currentState.currentPrice,
                ema21 = currentState.ema21,
                ema50 = currentState.ema50,
                trend = currentState.trend,
                obStatus = currentState.obStatus,
                fvgStatus = currentState.fvgStatus
            )
            val aiResponse = repository.askAiForAnalysis(request)
            _analysisState.value = _analysisState.value.copy(aiComment = aiResponse)
        }
    }
}