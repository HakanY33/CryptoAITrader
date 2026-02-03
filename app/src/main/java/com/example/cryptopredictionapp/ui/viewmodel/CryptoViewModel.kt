package com.example.cryptopredictionapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cryptopredictionapp.data.model.MarketDataRequest
import com.example.cryptopredictionapp.data.repository.CryptoRepository
import com.example.cryptopredictionapp.util.IndicatorUtils
import com.example.cryptopredictionapp.util.TechnicalAnalysis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.math.BigDecimal

// Ekrandaki verileri tutan durum sınıfı
data class AnalysisState(
    val currentPrice: String = "...",
    val ema21: String = "...",
    val ema50: String = "...",
    val obStatus: String = "...",
    val fvgStatus: String = "...",
    val trend: String = "Analiz Bekleniyor...", // Başlangıç metni değişti
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

    // --- ARAMA VE FİLTRELEME DEĞİŞKENLERİ ---
    private var allCoins = listOf<String>()

    // Ekranda gösterilen filtrelenmiş liste
    private val _filteredCoins = MutableStateFlow<List<String>>(emptyList())
    val filteredCoins: StateFlow<List<String>> = _filteredCoins.asStateFlow()

    // Arama çubuğundaki metin
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    // Arama modunda mıyız?
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Seçili Coin (Varsayılan BTC)
    private val _selectedSymbol = MutableStateFlow("BTC-USDT")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    // Seçili Zaman Dilimi (Varsayılan 1 Saat)
    private val _selectedTimeframe = MutableStateFlow("1h")
    val selectedTimeframe: StateFlow<String> = _selectedTimeframe.asStateFlow()

    // --- ANALİZ SONUÇLARI (STATE) ---
    private val _analysisState = MutableStateFlow(AnalysisState())
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Canlı takip işini yapan arka plan görevi
    private var livePriceJob: Job? = null

    init {
        loadCoinList()
        // Uygulama açılır açılmaz BTC seçili gelir, analiz yapalım
        analyzeMarket("BTC-USDT")
        // Arka planda fiyat akışını başlatalım
        startLightweightMonitoring()
    }

    // Tüm coin listesini API'den çeker
    private fun loadCoinList() {
        viewModelScope.launch {
            allCoins = repository.getAllSymbols()
            _filteredCoins.value = emptyList()
        }
    }

    // Arama kutusuna yazı yazıldığında çalışır
    fun onSearchTextChange(text: String) {
        _searchText.value = text
        _isSearching.value = true
        if (text.isEmpty()) {
            _filteredCoins.value = allCoins
        } else {
            _filteredCoins.value = allCoins.filter { it.contains(text, ignoreCase = true) }
        }
    }

    // Arama kutusuna tıklandığında listeyi aç
    fun onSearchFocus() {
        _isSearching.value = true
        if (_searchText.value.isEmpty()) _filteredCoins.value = allCoins
    }

    // Listeden bir coin seçildiğinde
    fun onCoinSelected(symbol: String) {
        _selectedSymbol.value = symbol
        _searchText.value = symbol
        _isSearching.value = false

        // Coin değişince tam analiz yap
        analyzeMarket(symbol)
    }

    // Zaman dilimi (15m, 1h, 4h) değiştiğinde
    fun onTimeframeSelected(interval: String) {
        _selectedTimeframe.value = interval
        // Zaman dilimi değişince tam analiz yap
        analyzeMarket(_selectedSymbol.value)
    }

    // --- 1. HAFİF MOD: SADECE FİYAT TAKİBİ (KASMA YAPMAZ) ---
    private fun startLightweightMonitoring() {
        livePriceJob?.cancel()
        livePriceJob = viewModelScope.launch {
            while (isActive) {
                try {
                    // Sadece fiyatı çek (Ağır hesaplama yok)
                    val priceStr = repository.getMarketPrice(_selectedSymbol.value)

                    // Sadece fiyatı güncelle, diğer verilere dokunma
                    _analysisState.update { it.copy(currentPrice = priceStr) }

                } catch (e: Exception) {
                    println("Fiyat akışı hatası: ${e.message}")
                }
                delay(2000) // 2 Saniyede bir fiyat güncelle
            }
        }
    }

    // --- 2. AĞIR MOD: DETAYLI ANALİZ (SADECE BUTONA BASINCA) ---
    fun analyzeMarket(symbol: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedSymbol.value = symbol

            try {
                // Mumları çek
                val interval = _selectedTimeframe.value
                val rawCandles = repository.getKlinesData(symbol, interval)
                val currentTicker = repository.getCryptoPrice(symbol)

                if (rawCandles.isNotEmpty() && currentTicker != null) {
                    // Verileri Matematiksel İşlem İçin Hazırla
                    val candlesReversed = rawCandles.reversed()
                    val closes = candlesReversed.map { BigDecimal(it.close) }
                    val highs = candlesReversed.map { BigDecimal(it.high) }
                    val lows = candlesReversed.map { BigDecimal(it.low) }
                    val volumes = candlesReversed.map { BigDecimal(it.volume) }

                    // --- 6 STRATEJİ OYLAMASI ---
                    var longVotes = 0
                    var shortVotes = 0

                    // 1. STRATEJİ: EMA
                    val ema21 = IndicatorUtils.calculateEMA(closes, 21)
                    val ema50 = IndicatorUtils.calculateEMA(closes, 50)
                    if (ema21 != null && ema50 != null) {
                        if (ema21 > ema50) longVotes++ else shortVotes++
                    }

                    // 2. STRATEJİ: ALLIGATOR
                    val alligator = IndicatorUtils.calculateAlligator(closes)
                    if (alligator != null) {
                        val (jaw, teeth, lips) = alligator
                        if (lips > teeth && teeth > jaw) longVotes++
                        if (jaw > teeth && teeth > lips) shortVotes++
                    }

                    // 3. STRATEJİ: MFI + CMF
                    val mfi = IndicatorUtils.calculateMFI(highs, lows, closes, volumes)
                    val cmf = IndicatorUtils.calculateCMF(highs, lows, closes, volumes)
                    if (mfi != null && cmf != null) {
                        if (mfi > BigDecimal(50) && cmf > BigDecimal(0.05)) longVotes++
                        if (mfi < BigDecimal(50) && cmf < BigDecimal(-0.05)) shortVotes++
                    }

                    // 4. STRATEJİ: AROON
                    val aroon = IndicatorUtils.calculateAroon(highs, lows)
                    if (aroon != null) {
                        val (up, down) = aroon
                        if (up > BigDecimal(70) && up > down) longVotes++
                        if (down > BigDecimal(70) && down > up) shortVotes++
                    }

                    // 5. STRATEJİ: RSI
                    val rsi = IndicatorUtils.calculateRSI(closes)
                    if (rsi != null) {
                        if (rsi > BigDecimal(50)) longVotes++ else shortVotes++
                    }

                    // 6. STRATEJİ: ADX + OBV
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

                    // --- SMC ANALİZİ ---
                    val obStatus = TechnicalAnalysis.findOrderBlock(rawCandles)
                    val fvgStatus = TechnicalAnalysis.findFVG(rawCandles)

                    // --- GENEL TREND KARARI ---
                    var trendText = "YATAY / BELİRSİZ"
                    var signalText = "İşlem Açma (Bekle)"
                    val scoreDisplay = "L:$longVotes / S:$shortVotes"

                    if (longVotes >= 4) {
                        trendText = "YÜKSELİŞ EĞİLİMİ 🟢"
                        signalText = "LONG Fırsatı (Skor: $longVotes/6)"
                    } else if (shortVotes >= 4) {
                        trendText = "DÜŞÜŞ EĞİLİMİ 🔴"
                        signalText = "SHORT Fırsatı (Skor: $shortVotes/6)"
                    }

                    // --- AKILLI GİRİŞ HESAPLA ---
                    var entry = ""; var tp = ""; var sl = ""
                    val atr = IndicatorUtils.calculateATR(highs, lows, closes)

                    if (atr != null) {
                        val currentBigDec = BigDecimal(currentTicker.lastPrice)
                        val setup = TechnicalAnalysis.calculateSmartTradeSetup(
                            currentPrice = currentBigDec,
                            atr = atr,
                            trend = trendText,
                            obString = obStatus,
                            fvgString = fvgStatus
                        )
                        entry = setup.first
                        tp = setup.second
                        sl = setup.third
                    }

                    // SONUÇLARI GÜNCELLE
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
                        candles = rawCandles
                    )
                } else {
                    _analysisState.value = _analysisState.value.copy(recommendation = "Veri Alınamadı")
                }
            } catch (e: Exception) {
                _analysisState.value = _analysisState.value.copy(recommendation = "Hata: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Kullanıcı "Yapay Zeka Yorumla" butonuna basarsa
    fun askAiCurrentState(symbol: String) {
        val currentState = _analysisState.value
        // Eğer analiz yapılmadıysa önce analiz yap
        if (currentState.trend == "Analiz Bekleniyor...") {
            analyzeMarket(symbol)
        }

        viewModelScope.launch {
            _analysisState.value = _analysisState.value.copy(aiComment = "Yapay Zeka Stratejini İnceliyor... 🤖")
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

    // Ekran kapanırsa döngüyü durdur
    override fun onCleared() {
        super.onCleared()
        livePriceJob?.cancel()
    }

    // --- İŞLEM YÖNETİMİ ---
    private val _tradeResult = MutableStateFlow<String?>(null)
    val tradeResult: StateFlow<String?> = _tradeResult.asStateFlow()

    private val _userLeverage = MutableStateFlow(20f)
    val userLeverage: StateFlow<Float> = _userLeverage.asStateFlow()

    fun onLeverageChanged(value: Float) {
        _userLeverage.value = value
    }

    // Metni Sayıya Çevir (Virgül/Nokta karmaşasını çözer)
    private fun parsePrice(input: String): Double {
        return try {
            val cleanStr = input.replace(Regex("[^0-9.,]"), "")
            val dotStr = cleanStr.replace(",", ".")
            dotStr.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    fun executeMarketTrade(side: String, tpText: String, slText: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _tradeResult.value = "İşlem ve TP/SL Hazırlanıyor..."

            val currentPrice = parsePrice(_analysisState.value.currentPrice)
            val takeProfit = parsePrice(tpText)
            val stopLoss = parsePrice(slText)

            println("DEBUG: İşlem: $side, Fiyat: $currentPrice, TP: $takeProfit, SL: $stopLoss")

            if (currentPrice > 0) {
                val result = repository.placeSmartTrade(
                    symbol = _selectedSymbol.value,
                    side = side,
                    price = currentPrice,
                    leverage = _userLeverage.value.toInt(),
                    tpPrice = takeProfit,
                    slPrice = stopLoss
                )
                _tradeResult.value = result
            } else {
                _tradeResult.value = "❌ Fiyat verisi alınamadı!"
            }

            delay(4000)
            _tradeResult.value = null
            _isLoading.value = false
        }
    }
}