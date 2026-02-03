package com.example.cryptopredictionapp.util

import com.example.cryptopredictionapp.data.model.BingxKlineData
import java.math.BigDecimal
import java.math.RoundingMode

object TechnicalAnalysis {

    // --- FVG BULUCU (AYNI KALDI) ---
    fun findFVG(candles: List<BingxKlineData>): String {
        if (candles.size < 50) return "Veri Az"
        val reversedCandles = candles.reversed()
        var bullishFvg: String? = null
        var bearishFvg: String? = null

        for (i in 1 until 50) {
            if (i + 2 >= reversedCandles.size) break
            val c1 = reversedCandles[i + 2]; val c3 = reversedCandles[i]
            val high1 = BigDecimal(c1.high); val low1 = BigDecimal(c1.low)
            val high3 = BigDecimal(c3.high); val low3 = BigDecimal(c3.low)

            // Bullish FVG
            if (low3 > high1) {
                var mitigated = false
                for (j in 0 until i) if (BigDecimal(reversedCandles[j].low) <= high1) mitigated = true
                if (!mitigated) {
                    bullishFvg = "${IndicatorUtils.formatPrice(high1)} - ${IndicatorUtils.formatPrice(low3)}"
                    break
                }
            }
            // Bearish FVG
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

    // --- OB BULUCU (GÜNCELLENDİ: FİYATI DA DÖNDÜRÜYORUZ) ---
    // Artık sadece String değil, hesaplama yapmak için Pair(String, BigDecimal?) döndüreceğiz.
    // Ancak yapıyı bozmamak için String döndürüp fiyatı setup içinde parse edeceğiz (Daha güvenli).
    fun findOrderBlock(candles: List<BingxKlineData>): String {
        if (candles.size < 50) return "Veri Az"
        val reversedCandles = candles.reversed()

        for (i in 1 until 50) {
            val current = reversedCandles[i]; val prev = reversedCandles[i+1]
            val cClose = BigDecimal(current.close); val cOpen = BigDecimal(current.open)
            val pOpen = BigDecimal(prev.open); val pClose = BigDecimal(prev.close)
            val pHigh = BigDecimal(prev.high); val pLow = BigDecimal(prev.low)

            val body = (cClose.subtract(cOpen)).abs()
            val prevBody = (pClose.subtract(pOpen)).abs()

            // Bullish OB (Düşüş mumunu yutan yükseliş)
            if (cClose > cOpen && body > prevBody) {
                if (pClose < pOpen) { // Önceki Kırmızı
                    var mitigated = false
                    for (j in 0 until i) if (BigDecimal(reversedCandles[j].low) < pHigh) mitigated = true
                    if (!mitigated) return "Bull OB: ${IndicatorUtils.formatPrice(pHigh)}" // Giriş yeri: OB'nin tepesi
                }
            }
            // Bearish OB
            if (cClose < cOpen && body > prevBody) {
                if (pClose > pOpen) { // Önceki Yeşil
                    var mitigated = false
                    for (j in 0 until i) if (BigDecimal(reversedCandles[j].high) > pLow) mitigated = true
                    if (!mitigated) return "Bear OB: ${IndicatorUtils.formatPrice(pLow)}" // Giriş yeri: OB'nin altı
                }
            }
        }
        return "Yakın OB Yok"
    }

    // --- AKILLI TRADE SETUP (SNIPER MODU) ---
    fun calculateSmartTradeSetup(
        currentPrice: BigDecimal,
        atr: BigDecimal,
        trend: String,
        obString: String,  // OB bilgisini alıyoruz
        fvgString: String  // FVG bilgisini alıyoruz
    ): Triple<String, String, String> { // Entry, TP, SL

        var entryPrice = currentPrice
        var entryReason = "(Market)" // Ekranda göstermek için

        // 1. GİRİŞ YERİNİ İYİLEŞTİRME (Smart Entry)
        // Eğer Trend Yükselişse ve elimizde Bullish OB varsa, girişi oraya çek.
        if (trend.contains("YÜKSELİŞ") || trend.contains("LONG")) {
            if (obString.contains("Bull OB:")) {
                // String içinden fiyatı çekiyoruz: "Bull OB: 65000.50" -> 65000.50
                val obPriceStr = obString.substringAfter("Bull OB:").trim()
                val obPrice = obPriceStr.toBigDecimalOrNull()

                // Eğer OB fiyatı şu anki fiyatın altındaysa (yani geri çekilme bekleniyorsa)
                if (obPrice != null && obPrice < currentPrice) {
                    entryPrice = obPrice
                    entryReason = "(Limit: OB)"
                }
            }
        }
        // Short Mantığı
        else if (trend.contains("DÜŞÜŞ") || trend.contains("SHORT")) {
            if (obString.contains("Bear OB:")) {
                val obPriceStr = obString.substringAfter("Bear OB:").trim()
                val obPrice = obPriceStr.toBigDecimalOrNull()

                if (obPrice != null && obPrice > currentPrice) {
                    entryPrice = obPrice
                    entryReason = "(Limit: OB)"
                }
            }
        }

        // 2. TP / SL HESAPLAMA (ATR Bazlı)
        // Stop Loss: Girişten 2 ATR uzaklıkta
        // Take Profit: Girişten 4 ATR uzaklıkta (Risk/Reward 1:2)
        val slDist = atr.multiply(BigDecimal(2))
        val tpDist = atr.multiply(BigDecimal(4))

        val stopLoss: BigDecimal
        val takeProfit: BigDecimal

        if (trend.contains("YÜKSELİŞ") || trend.contains("LONG")) {
            stopLoss = entryPrice.subtract(slDist)
            takeProfit = entryPrice.add(tpDist)
        } else {
            stopLoss = entryPrice.add(slDist)
            takeProfit = entryPrice.subtract(tpDist)
        }

        return Triple(
            "${IndicatorUtils.formatPrice(entryPrice)} $entryReason",
            IndicatorUtils.formatPrice(takeProfit),
            IndicatorUtils.formatPrice(stopLoss)
        )
    }
}