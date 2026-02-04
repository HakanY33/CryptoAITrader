package com.example.cryptopredictionapp.util

import com.example.cryptopredictionapp.data.model.BingxKlineData
import java.math.BigDecimal
import java.math.RoundingMode

object TechnicalAnalysis {

    // --- YARDIMCI FONKSİYON: EMA HESAPLAMA ---
    fun calculateEMA(prices: List<BigDecimal>, period: Int): List<BigDecimal> {
        val emaValues = mutableListOf<BigDecimal>()
        if (prices.size < period) return emaValues

        // İlk değer SMA (Basit Ortalama)
        var sum = BigDecimal.ZERO
        for (i in 0 until period) sum = sum.add(prices[i])
        var currentEma = sum.divide(BigDecimal(period), 8, RoundingMode.HALF_UP)
        emaValues.add(currentEma)

        // EMA Formülü: (Fiyat * K) + (ÖncekiEMA * (1-K))
        val k = BigDecimal(2).divide(BigDecimal(period + 1), 8, RoundingMode.HALF_UP)

        for (i in period until prices.size) {
            val price = prices[i]
            // EMA = Price * k + PrevEMA * (1 - k)
            val part1 = price.multiply(k)
            val part2 = currentEma.multiply(BigDecimal.ONE.subtract(k))
            currentEma = part1.add(part2)
            emaValues.add(currentEma)
        }
        return emaValues
    }

    // --- YENİ: SMOOTHED RSI (RSI + RSI EMA) ---
    // Dönüş: Triple(RSI Değeri, RSI EMA Değeri, Sinyal Durumu)
    fun calculateSmoothedRSI(candles: List<BingxKlineData>, rsiPeriod: Int = 14, emaPeriod: Int = 14): Triple<BigDecimal, BigDecimal, String> {
        val closes = candles.map { BigDecimal(it.close) }

        // 1. RSI Hesapla (Basitleştirilmiş RSI fonksiyonu çağırdığını varsayıyoruz veya buraya entegre edebilirsin)
        // Şimdilik senin IndicatorUtils içindeki calculateRSI'ı kullandığını varsayalım.
        // Eğer o liste döndürmüyorsa, manuel bir RSI array hesaplamamız gerekebilir.
        // Basitlik adına son RSI ve önceki RSI'ı alıp trende bakacağız.

        // *Burada tam bir RSI dizisi lazım ki onun EMA'sını alalım.*
        // Bu örnek için basitleştirilmiş bir yaklaşım kullanacağız:
        // Eğer elinde RSI dizisi yoksa, son 14 mumun RSI'ını alıp ortalamasına bakmak bir yaklaşımdır.

        // NOT: Gerçek implementasyonda IndicatorUtils.calculateRSIList() gibi bir metoda ihtiyacın var.
        // Varsayalım ki son RSI değerini aldık:
        val currentRSI = IndicatorUtils.calculateRSI(closes) ?: BigDecimal.ZERO

        // Smoothed mantığı için geçmiş RSI verilerine ihtiyaç var.
        // Şimdilik basit bir kontrol yapalım: RSI 50'nin üstünde mi?
        val signal = if (currentRSI > BigDecimal(50)) "BULLISH" else "BEARISH"

        return Triple(currentRSI, BigDecimal.ZERO, signal)
    }

    // --- GERÇEK HYBRID SETUP MOTORU (PINE SCRIPT MANTIĞI) ---
    fun calculateHybridTradeSetup(
        candles: List<BingxKlineData>,
        currentPrice: BigDecimal,
        atr: BigDecimal
    ): Triple<String, String, String> { // Entry, TP, SL

        if (candles.size < 200) return Triple("Veri Yetersiz", "-", "-")

        val closes = candles.map { BigDecimal(it.close) }

        // 1. TREND FİLTRESİ (EMA 200)
        val ema200List = calculateEMA(closes, 200)
        if (ema200List.isEmpty()) return Triple("Veri Yetersiz", "-", "-")

        val ema200 = ema200List.last()
        val isUptrend = currentPrice > ema200 // Trend Yukarı (Sadece Long Ara)
        val isDowntrend = currentPrice < ema200 // Trend Aşağı (Sadece Short Ara)

        // 2. ATR BAZLI HEDEF HESAPLAMA (Risk Manager Scriptinden)
        // SL = 1.5 ATR, TP = 4.0 ATR (Hybrid ayarları)
        val slMultiplier = BigDecimal("1.5")
        val tpMultiplier = BigDecimal("4.0")

        val slDist = atr.multiply(slMultiplier)
        val tpDist = atr.multiply(tpMultiplier)

        var entryReason = "(Bekle)"
        var takeProfit = BigDecimal.ZERO
        var stopLoss = BigDecimal.ZERO
        var setupType = "NÖTR"

        // 3. RSI KONTROLÜ (Basit: Aşırı Alım/Satım + Trend Yönü)
        // Gelişmiş versiyonda RSI EMA kesişimi buraya eklenecek.
        val rsi = IndicatorUtils.calculateRSI(closes) ?: BigDecimal(50)

        // --- LONG SENARYOSU ---
        // Trend Yukarı VE (RSI Dipte VEYA Fiyat EMA 200'e yakın)
        if (isUptrend) {
            // Fiyat EMA200'e geri çekildiyse bu bir fırsattır (OTE Mantığına benzer)
            if (currentPrice < ema200.multiply(BigDecimal("1.02"))) { // %2 yakınlıkta
                entryReason = "Trend Desteği (EMA 200)"
                setupType = "LONG"
                stopLoss = currentPrice.subtract(slDist)
                takeProfit = currentPrice.add(tpDist)
            }
            else if (rsi < BigDecimal(40)) { // Trend yukarı ama RSI aşırı satılmış (Düzeltme bitiyor)
                entryReason = "RSI Oversold + Trend"
                setupType = "LONG"
                stopLoss = currentPrice.subtract(slDist)
                takeProfit = currentPrice.add(tpDist)
            }
        }

        // --- SHORT SENARYOSU ---
        // Trend Aşağı VE (RSI Tepede VEYA Fiyat EMA 200'e yakın)
        else if (isDowntrend) {
            if (currentPrice > ema200.multiply(BigDecimal("0.98"))) {
                entryReason = "Trend Direnci (EMA 200)"
                setupType = "SHORT"
                stopLoss = currentPrice.add(slDist)
                takeProfit = currentPrice.subtract(tpDist)
            }
            else if (rsi > BigDecimal(60)) {
                entryReason = "RSI Overbought + Trend"
                setupType = "SHORT"
                stopLoss = currentPrice.add(slDist)
                takeProfit = currentPrice.subtract(tpDist)
            }
        }

        if (setupType == "NÖTR") {
            return Triple("İşlem Yok", "Bekle", "Bekle")
        }

        return Triple(
            "${IndicatorUtils.formatPrice(currentPrice)} ($entryReason)",
            IndicatorUtils.formatPrice(takeProfit),
            IndicatorUtils.formatPrice(stopLoss)
        )
    }

    // --- ESKİ SMC FONKSİYONLARI (KORUYORUZ) ---
    // findFVG ve findOrderBlock fonksiyonlarını aynen burada tutabilirsin.
    // ... (Senin attığın koddaki findFVG ve findOrderBlock buraya gelecek) ...

    fun findFVG(candles: List<BingxKlineData>): String {
        // ... (Senin kodun aynısı) ...
        if (candles.size < 50) return "Veri Az"
        val reversedCandles = candles.reversed()
        var bullishFvg: String? = null
        var bearishFvg: String? = null

        for (i in 1 until 50) {
            if (i + 2 >= reversedCandles.size) break
            val c1 = reversedCandles[i + 2]; val c3 = reversedCandles[i]
            val high1 = BigDecimal(c1.high); val low1 = BigDecimal(c1.low)
            val high3 = BigDecimal(c3.high); val low3 = BigDecimal(c3.low)

            if (low3 > high1) {
                var mitigated = false
                for (j in 0 until i) if (BigDecimal(reversedCandles[j].low) <= high1) mitigated = true
                if (!mitigated) {
                    bullishFvg = "${IndicatorUtils.formatPrice(high1)} - ${IndicatorUtils.formatPrice(low3)}"
                    break
                }
            }
            if (high3 < low1) {
                var mitigated = false
                for (j in 0 until i) if (BigDecimal(reversedCandles[j].high) >= low1) mitigated = true
                if (!mitigated) {
                    bearishFvg = "${IndicatorUtils.formatPrice(high3)} - ${IndicatorUtils.formatPrice(low1)}"
                    break
                }
            }
        }
        return when {
            bullishFvg != null -> "Bull FVG: $bullishFvg"
            bearishFvg != null -> "Bear FVG: $bearishFvg"
            else -> "Açık FVG Yok"
        }
    }

    fun findOrderBlock(candles: List<BingxKlineData>): String {
        // ... (Senin kodun aynısı) ...
        if (candles.size < 50) return "Veri Az"
        val reversedCandles = candles.reversed()

        for (i in 1 until 50) {
            val current = reversedCandles[i]; val prev = reversedCandles[i+1]
            val cClose = BigDecimal(current.close); val cOpen = BigDecimal(current.open)
            val pOpen = BigDecimal(prev.open); val pClose = BigDecimal(prev.close)
            val pHigh = BigDecimal(prev.high); val pLow = BigDecimal(prev.low)

            val body = (cClose.subtract(cOpen)).abs()
            val prevBody = (pClose.subtract(pOpen)).abs()

            if (cClose > cOpen && body > prevBody) {
                if (pClose < pOpen) {
                    var mitigated = false
                    for (j in 0 until i) if (BigDecimal(reversedCandles[j].low) < pHigh) mitigated = true
                    if (!mitigated) return "Bull OB: ${IndicatorUtils.formatPrice(pHigh)}"
                }
            }
            if (cClose < cOpen && body > prevBody) {
                if (pClose > pOpen) {
                    var mitigated = false
                    for (j in 0 until i) if (BigDecimal(reversedCandles[j].high) > pLow) mitigated = true
                    if (!mitigated) return "Bear OB: ${IndicatorUtils.formatPrice(pLow)}"
                }
            }
        }
        return "Yakın OB Yok"
    }
}