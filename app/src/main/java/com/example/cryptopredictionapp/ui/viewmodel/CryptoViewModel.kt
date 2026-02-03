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
        // Uygulama açılır açılmaz BTC için canlı takibi başlat
        startRealTimeUpdates("BTC-USDT")
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
        // Seçilen coin için canlı takibi başlat
        startRealTimeUpdates(symbol)
    }

    // Zaman dilimi (15m, 1h, 4h) değiştiğinde
    fun onTimeframeSelected(interval: String) {
        _selectedTimeframe.value = interval
        // Yeni zaman dilimine göre grafiği ve analizi güncelle
        startRealTimeUpdates(_selectedSymbol.value)
    }

    // Manuel olarak "Analiz Et" butonuna basılırsa (Aslında otomatik ama yine de dursun)
    fun analyzeMarket(symbol: String) {
        startRealTimeUpdates(symbol)
    }

    // --- CANLI TAKİP MOTORU (BEYİN BURASI) ---
    // Bu fonksiyon sürekli döngü halinde çalışır ve her 3 saniyede bir analiz yapar.
    private fun startRealTimeUpdates(symbol: String) {
        // Eğer önceki bir takip varsa durdur (Çakışma olmasın)
        livePriceJob?.cancel()

        livePriceJob = viewModelScope.launch {
            _isLoading.value = true // İlk başta yükleniyor göster

            // Sonsuz döngü (Ekran kapanana kadar)
            while (isActive) {
                val interval = _selectedTimeframe.value

                // 1. Verileri Çek (Mumlar ve Anlık Fiyat)
                val rawCandles = repository.getKlinesData(symbol, interval)
                val currentTicker = repository.getCryptoPrice(symbol)

                if (rawCandles.isNotEmpty() && currentTicker != null) {
                    // Verileri Matematiksel İşlem İçin Hazırla
                    val candlesReversed = rawCandles.reversed() // Eskiden yeniye sırala
                    val closes = candlesReversed.map { BigDecimal(it.close) }
                    val highs = candlesReversed.map { BigDecimal(it.high) }
                    val lows = candlesReversed.map { BigDecimal(it.low) }
                    val volumes = candlesReversed.map { BigDecimal(it.volume) }

                    // --- 6 STRATEJİ OYLAMASI ---
                    // Her indikatör bir oy kullanır: Long veya Short
                    var longVotes = 0
                    var shortVotes = 0

                    // 1. STRATEJİ: EMA (Hareketli Ortalamalar)
                    // Kısa vade (21), Uzun vadeyi (50) yukarı keserse AL
                    val ema21 = IndicatorUtils.calculateEMA(closes, 21)
                    val ema50 = IndicatorUtils.calculateEMA(closes, 50)
                    if (ema21 != null && ema50 != null) {
                        if (ema21 > ema50) longVotes++ else shortVotes++
                    }

                    // 2. STRATEJİ: ALLIGATOR (Williams Timsahı)
                    // Timsahın ağzı yukarı açıksa AL, aşağı açıksa SAT
                    val alligator = IndicatorUtils.calculateAlligator(closes)
                    if (alligator != null) {
                        val (jaw, teeth, lips) = alligator
                        if (lips > teeth && teeth > jaw) longVotes++
                        if (jaw > teeth && teeth > lips) shortVotes++
                    }

                    // 3. STRATEJİ: MFI + CMF (Para Akışı)
                    // Para girişi varsa AL, para çıkışı varsa SAT
                    val mfi = IndicatorUtils.calculateMFI(highs, lows, closes, volumes)
                    val cmf = IndicatorUtils.calculateCMF(highs, lows, closes, volumes)
                    if (mfi != null && cmf != null) {
                        if (mfi > BigDecimal(50) && cmf > BigDecimal(0.05)) longVotes++
                        if (mfi < BigDecimal(50) && cmf < BigDecimal(-0.05)) shortVotes++
                    }

                    // 4. STRATEJİ: AROON (Trend Gücü)
                    // Yükseliş trendi güçlüyse AL
                    val aroon = IndicatorUtils.calculateAroon(highs, lows)
                    if (aroon != null) {
                        val (up, down) = aroon
                        if (up > BigDecimal(70) && up > down) longVotes++
                        if (down > BigDecimal(70) && down > up) shortVotes++
                    }

                    // 5. STRATEJİ: RSI (Aşırı Alım/Satım)
                    // 50'nin üzerindeyse Trend Güçlü (AL)
                    val rsi = IndicatorUtils.calculateRSI(closes)
                    if (rsi != null) {
                        if (rsi > BigDecimal(50)) longVotes++ else shortVotes++
                    }

                    // 6. STRATEJİ: ADX + OBV (Trend ve Hacim Onayı)
                    // Trend güçlüyse (ADX > 25) ve Hacim destekliyorsa (OBV)
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

                    // --- SMC ANALİZİ (Order Block & FVG) ---
                    val obStatus = TechnicalAnalysis.findOrderBlock(rawCandles)
                    val fvgStatus = TechnicalAnalysis.findFVG(rawCandles)

                    // --- GENEL TREND KARARI ---
                    var trendText = "YATAY / BELİRSİZ"
                    var signalText = "İşlem Açma (Bekle)"
                    val scoreDisplay = "L:$longVotes / S:$shortVotes" // Ekranda skor gösterimi

                    // Eğer 6 stratejiden en az 4'ü aynı fikirdeyse Sinyal Üret
                    if (longVotes >= 4) {
                        trendText = "YÜKSELİŞ EĞİLİMİ 🟢"
                        signalText = "LONG Fırsatı (Skor: $longVotes/6)"
                    } else if (shortVotes >= 4) {
                        trendText = "DÜŞÜŞ EĞİLİMİ 🔴"
                        signalText = "SHORT Fırsatı (Skor: $shortVotes/6)"
                    }

                    // --- AKILLI GİRİŞ (SMART SETUP) HESAPLA ---
                    var entry = ""; var tp = ""; var sl = ""
                    val atr = IndicatorUtils.calculateATR(highs, lows, closes)

                    if (atr != null) {
                        val currentBigDec = BigDecimal(currentTicker.lastPrice)
                        // Trende ve OB durumuna göre en iyi giriş yerini hesapla
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

                    // 2. SONUÇLARI EKRANA BAS (State Güncelle)
                    _analysisState.value = AnalysisState(
                        currentPrice = IndicatorUtils.formatPrice(BigDecimal(currentTicker.lastPrice)),
                        ema21 = IndicatorUtils.formatPrice(ema21),
                        ema50 = IndicatorUtils.formatPrice(ema50),
                        obStatus = obStatus,
                        fvgStatus = fvgStatus,
                        trend = trendText,
                        recommendation = signalText,
                        strategyScore = scoreDisplay,
                        aiComment = "", // AI yorumu sadece butona basınca gelir, burayı boş bırakıyoruz
                        tradeEntry = entry,
                        tradeTp = tp,
                        tradeSl = sl,
                        candles = rawCandles // Grafiği çizdirmek için mumları gönder
                    )
                } else {
                    _analysisState.value = _analysisState.value.copy(recommendation = "Veri Alınamadı")
                }

                _isLoading.value = false
                // 3 saniye bekle ve tekrar başa dön (Canlı Grafik Hissi)
                delay(3000)
            }
        }
    }

    // Kullanıcı "Yapay Zeka Yorumla" butonuna basarsa
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

    // Ekran kapanırsa döngüyü durdur (Pil tasarrufu)
    override fun onCleared() {
        super.onCleared()
        livePriceJob?.cancel()
    }
}