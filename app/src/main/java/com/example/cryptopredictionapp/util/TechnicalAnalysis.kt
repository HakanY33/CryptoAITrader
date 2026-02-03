package com.example.cryptopredictionapp.util

import com.example.cryptopredictionapp.data.model.BingxKlineData
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

object TechnicalAnalysis {

    // --- FAIR VALUE GAP (FVG) BULUCU ---
    // Son 200 mumu tarar, test edilmemiş (açık) en yakın FVG'yi bulur.
    fun findFVG(candles: List<BingxKlineData>): String {
        if (candles.size < 50) return "Veri Az"

        // Mumları Eskiden -> Yeniye sıralı varsayıyoruz (API genelde böyle verir)
        // Ama işleme kolaylığı için ters çevirelim (Index 0 = En Son Mum)
        val reversedCandles = candles.reversed()
        val currentPrice = BigDecimal(reversedCandles[0].close)

        // Aktif (Test edilmemiş) FVG'leri tutacak listeler
        var bullishFvg: String? = null
        var bearishFvg: String? = null

        // Son 50 muma bakmak genelde yeterlidir (fazlası çok eski olur)
        for (i in 1 until 50) {
            // FVG için en az 3 mum lazım: i (son), i+1 (orta), i+2 (ilk)
            if (i + 2 >= reversedCandles.size) break

            val candle1 = reversedCandles[i + 2] // Sol
            val candle2 = reversedCandles[i + 1] // Orta (FVG'yi oluşturan hareket)
            val candle3 = reversedCandles[i]     // Sağ

            val high1 = BigDecimal(candle1.high)
            val low1 = BigDecimal(candle1.low)
            val high3 = BigDecimal(candle3.high)
            val low3 = BigDecimal(candle3.low)

            // --- BULLISH FVG (Yükseliş Boşluğu) ---
            // Kural: 1. mumun yükseği < 3. mumun düşüğü
            if (low3 > high1) {
                // Bu boşluk daha sonraki mumlar (i-1, i-2... 0) tarafından dolduruldu mu?
                var isMitigated = false
                for (j in 0 until i) {
                    val futureLow = BigDecimal(reversedCandles[j].low)
                    // Eğer gelecek mumların iğnesi, boşluğun içine girdiyse "Mitigated" sayılır
                    if (futureLow <= high1) {
                        isMitigated = true
                        break
                    }
                }

                if (!isMitigated) {
                    // Bulduk! Fiyat buraya geri çekilirse LONG fırsatıdır.
                    // Format: "FVG: 68100 - 68500"
                    bullishFvg = "${IndicatorUtils.formatPrice(high1)} - ${IndicatorUtils.formatPrice(low3)}"
                    // En yakın olanı bulduğumuz an döngüden çıkmıyoruz,
                    // ama genelde en son oluşan (i en küçük olan) en önemlisidir.
                    // Biz ilk bulduğumuzu (en güncelini) alıp çıkabiliriz.
                    if (bullishFvg != null) break
                }
            }

            // --- BEARISH FVG (Düşüş Boşluğu) ---
            // Kural: 1. mumun düşüğü > 3. mumun yükseği
            if (high3 < low1) {
                var isMitigated = false
                for (j in 0 until i) {
                    val futureHigh = BigDecimal(reversedCandles[j].high)
                    if (futureHigh >= low1) {
                        isMitigated = true
                        break
                    }
                }

                if (!isMitigated) {
                    bearishFvg = "${IndicatorUtils.formatPrice(high3)} - ${IndicatorUtils.formatPrice(low1)}"
                    if (bearishFvg != null) break
                }
            }
        }

        // Karar Anı: Fiyata hangisi yakınsa veya trende göre mantıklı olanı döndür
        return when {
            bullishFvg != null && bearishFvg != null -> "Bull: $bullishFvg / Bear: $bearishFvg"
            bullishFvg != null -> "Bullish FVG: $bullishFvg 🟢"
            bearishFvg != null -> "Bearish FVG: $bearishFvg 🔴"
            else -> "Açık FVG Yok"
        }
    }

    // --- ORDER BLOCK (OB) BULUCU ---
    // Son düşüşten önceki son yeşil mum (Bearish OB) veya son yükselişten önceki son kırmızı mum (Bullish OB)
    fun findOrderBlock(candles: List<BingxKlineData>): String {
        if (candles.size < 50) return "Veri Az"

        val reversedCandles = candles.reversed()
        val currentPrice = BigDecimal(reversedCandles[0].close)

        var bullishOB: String? = null
        var bearishOB: String? = null

        // Basitleştirilmiş Algoritma: Swing noktalarını bulmak zordur,
        // bu yüzden sert hareketleri (Marubozu veya uzun mumları) referans alacağız.

        for (i in 1 until 50) {
            val current = reversedCandles[i]
            val prev = reversedCandles[i+1] // OB adayı

            val cOpen = BigDecimal(current.open)
            val cClose = BigDecimal(current.close)
            val pOpen = BigDecimal(prev.open)
            val pClose = BigDecimal(prev.close)
            val pHigh = BigDecimal(prev.high)
            val pLow = BigDecimal(prev.low)

            // Hareketin büyüklüğü (ATR mantığı basitçe)
            val bodySize = (cClose.subtract(cOpen)).abs()
            val prevBodySize = (pClose.subtract(pOpen)).abs()

            // --- BULLISH OB ARAYIŞI ---
            // Sert bir yükseliş mumu (Yeşil) gördük. Ondan önceki mum Kırmızı mıydı?
            if (cClose > cOpen && bodySize > prevBodySize * BigDecimal(1.5)) {
                if (pClose < pOpen) { // Önceki mum Kırmızı
                    // Bu bölge test edildi mi? (Fiyat pHigh altına indi mi?)
                    var isMitigated = false
                    for (j in 0 until i) {
                        if (BigDecimal(reversedCandles[j].low) < pHigh) {
                            isMitigated = true // Basitçe: Fiyat oraya dokunduysa iptal et (Test edildi)
                            break
                        }
                    }
                    if (!isMitigated) {
                        bullishOB = "${IndicatorUtils.formatPrice(pLow)} - ${IndicatorUtils.formatPrice(pHigh)}"
                        break // En güncelini bulduk
                    }
                }
            }

            // --- BEARISH OB ARAYIŞI ---
            // Sert bir düşüş mumu (Kırmızı) gördük. Ondan önceki mum Yeşil miydi?
            if (cClose < cOpen && bodySize > prevBodySize * BigDecimal(1.5)) {
                if (pClose > pOpen) { // Önceki mum Yeşil
                    var isMitigated = false
                    for (j in 0 until i) {
                        if (BigDecimal(reversedCandles[j].high) > pLow) {
                            isMitigated = true
                            break
                        }
                    }
                    if (!isMitigated) {
                        bearishOB = "${IndicatorUtils.formatPrice(pLow)} - ${IndicatorUtils.formatPrice(pHigh)}"
                        break
                    }
                }
            }
        }

        return when {
            bullishOB != null && bearishOB != null -> "Bull: $bullishOB / Bear: $bearishOB"
            bullishOB != null -> "Bullish OB: $bullishOB 🟢"
            bearishOB != null -> "Bearish OB: $bearishOB 🔴"
            else -> "Yakın OB Yok"
        }
    }

    // --- TP / SL HESAPLAYICI (Sniper Setup) ---
    fun calculateTradeSetup(
        currentPrice: BigDecimal,
        atr: BigDecimal,
        trend: String
    ): Triple<String, String, String> {
        // Strateji:
        // Stop Loss = ATR * 2 (Gürültüden kaçmak için)
        // Take Profit = ATR * 5 (1'e 2.5 Risk/Ödül oranı)

        val slDist = atr.multiply(BigDecimal(2))
        val tpDist = atr.multiply(BigDecimal(5))

        val stopLoss: BigDecimal
        val takeProfit: BigDecimal
        val entryPrice = currentPrice // Şimdilik market giriş, limit emir için OB kullanılabilir

        if (trend.contains("YÜKSELİŞ")) {
            // Long Setup
            stopLoss = entryPrice.subtract(slDist)
            takeProfit = entryPrice.add(tpDist)
        } else {
            // Short Setup
            stopLoss = entryPrice.add(slDist)
            takeProfit = entryPrice.subtract(tpDist)
        }

        return Triple(
            IndicatorUtils.formatPrice(entryPrice),
            IndicatorUtils.formatPrice(takeProfit),
            IndicatorUtils.formatPrice(stopLoss)
        )
    }
}