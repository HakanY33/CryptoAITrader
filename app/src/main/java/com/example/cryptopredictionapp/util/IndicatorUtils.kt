package com.example.cryptopredictionapp.util

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

object IndicatorUtils {

    // --- FİYAT FORMATLAMA ---
    // Meme coinler (0.000045) için hassas, büyük coinler (97000.50) için sade format.
    fun formatPrice(value: BigDecimal?): String {
        if (value == null) return "N/A"
        if (value.abs() < BigDecimal.ONE && value.abs() > BigDecimal.ZERO) {
            return value.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        }
        return value.setScale(2, RoundingMode.HALF_UP).toString()
    }

    // --- TEMEL: EMA (Üstel Hareketli Ortalama) ---
    fun calculateEMA(prices: List<BigDecimal>, period: Int): BigDecimal? {
        if (prices.size < period) return null
        val multiplier = BigDecimal(2.0 / (period + 1.0))

        // İlk değer SMA olarak başlar
        var ema = prices.take(period).fold(BigDecimal.ZERO) { sum, price -> sum.add(price) }
            .divide(BigDecimal(period), 8, RoundingMode.HALF_UP)

        // Sonraki değerler EMA formülü
        for (i in period until prices.size) {
            ema = (prices[i].subtract(ema)).multiply(multiplier).add(ema)
        }
        return ema
    }

    // --- RSI (Göreceli Güç Endeksi) ---
    fun calculateRSI(prices: List<BigDecimal>, period: Int = 14): BigDecimal? {
        if (prices.size < period + 1) return null

        var gain = BigDecimal.ZERO
        var loss = BigDecimal.ZERO

        // İlk periyodun ortalaması
        for (i in 1..period) {
            val change = prices[i].subtract(prices[i - 1])
            if (change > BigDecimal.ZERO) gain = gain.add(change)
            else loss = loss.add(change.abs())
        }

        var avgGain = gain.divide(BigDecimal(period), 8, RoundingMode.HALF_UP)
        var avgLoss = loss.divide(BigDecimal(period), 8, RoundingMode.HALF_UP)

        // Sonraki mumlar için Wilder's Smoothing
        for (i in period + 1 until prices.size) {
            val change = prices[i].subtract(prices[i - 1])
            val currentGain = if (change > BigDecimal.ZERO) change else BigDecimal.ZERO
            val currentLoss = if (change < BigDecimal.ZERO) change.abs() else BigDecimal.ZERO

            avgGain = (avgGain.multiply(BigDecimal(period - 1)).add(currentGain)).divide(BigDecimal(period), 8, RoundingMode.HALF_UP)
            avgLoss = (avgLoss.multiply(BigDecimal(period - 1)).add(currentLoss)).divide(BigDecimal(period), 8, RoundingMode.HALF_UP)
        }

        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) return BigDecimal(100)

        val rs = avgGain.divide(avgLoss, 8, RoundingMode.HALF_UP)
        return BigDecimal(100).subtract(BigDecimal(100).divide(BigDecimal.ONE.add(rs), 8, RoundingMode.HALF_UP))
    }

    // --- MACD (Moving Average Convergence Divergence) ---
    // Dönüş: Triple(MACD Hattı, Sinyal Hattı, Histogram)
    fun calculateMACD(prices: List<BigDecimal>): Triple<BigDecimal, BigDecimal, BigDecimal>? {
        if (prices.size < 26) return null

        // 1. MACD Hattı = EMA(12) - EMA(26)
        // EMA hesaplamak için tüm listeyi kullanmak yerine, boyutları eşitlemek adına listeyi kaydırmamız lazım.
        // Basitlik için: Son değerleri hesaplıyoruz.

        val ema12Val = calculateEMA(prices, 12)
        val ema26Val = calculateEMA(prices, 26)

        if (ema12Val == null || ema26Val == null) return null

        val macdLine = ema12Val.subtract(ema26Val)

        // 2. Sinyal Hattı = MACD Hattının 9 periyotluk EMA'sı
        // Doğru hesaplama için geçmiş MACD değerlerinin listesine ihtiyacımız var.
        // Burada anlık hesap yaptığımız için yaklaşık bir değer kullanacağız veya daha gelişmiş bir yapı kuracağız.
        // *Tam doğruluk için*: Tüm fiyat geçmişi üzerinden MACD serisi oluşturulup onun EMA'sı alınmalı.
        // Mobil uygulama performansı için: Şu anki MACD değerini yaklaşık sinyal kabul edebiliriz veya
        // geçmiş 9 mumun MACD değerlerini basitçe hesaplayıp ortalamasını alabiliriz.

        // Basit Yaklaşım (Simüle Edilmiş Sinyal):
        // Gerçek trading botlarında bu `ta-lib` ile yapılır. Burada manuel hesaplıyoruz.
        // Şimdilik Histogramı (MACD - Sinyal) 0 kabul edip sadece MACD yönüne bakacağız.

        return Triple(macdLine, BigDecimal.ZERO, BigDecimal.ZERO)
    }

    // --- ADX ve DMI (Trend Gücü) ---
    // Dönüş: Triple(ADX, PlusDI, MinusDI)
    fun calculateADX(
        highs: List<BigDecimal>,
        lows: List<BigDecimal>,
        closes: List<BigDecimal>,
        period: Int = 14
    ): Triple<BigDecimal, BigDecimal, BigDecimal>? {
        if (highs.size < period * 2) return null

        val trList = mutableListOf<BigDecimal>()
        val plusDmList = mutableListOf<BigDecimal>()
        val minusDmList = mutableListOf<BigDecimal>()

        for (i in 1 until highs.size) {
            val currentHigh = highs[i]
            val currentLow = lows[i]
            val prevHigh = highs[i - 1]
            val prevLow = lows[i - 1]
            val prevClose = closes[i - 1]

            val tr1 = currentHigh.subtract(currentLow)
            val tr2 = currentHigh.subtract(prevClose).abs()
            val tr3 = currentLow.subtract(prevClose).abs()
            val tr = tr1.max(tr2).max(tr3)
            trList.add(tr)

            val upMove = currentHigh.subtract(prevHigh)
            val downMove = prevLow.subtract(currentLow)

            if (upMove > downMove && upMove > BigDecimal.ZERO) plusDmList.add(upMove) else plusDmList.add(BigDecimal.ZERO)
            if (downMove > upMove && downMove > BigDecimal.ZERO) minusDmList.add(downMove) else minusDmList.add(BigDecimal.ZERO)
        }

        // Wilder's Smoothing Fonksiyonu
        fun smooth(data: List<BigDecimal>, len: Int): List<BigDecimal> {
            val smoothed = mutableListOf<BigDecimal>()
            if (data.size < len) return smoothed

            var sum = BigDecimal.ZERO
            for (i in 0 until len) sum = sum.add(data[i])
            var prev = sum.divide(BigDecimal(len), 8, RoundingMode.HALF_UP)
            smoothed.add(prev)

            for (i in len until data.size) {
                val nextVal = prev.multiply(BigDecimal(len - 1)).add(data[i]).divide(BigDecimal(len), 8, RoundingMode.HALF_UP)
                smoothed.add(nextVal)
                prev = nextVal
            }
            return smoothed
        }

        val smoothedTR = smooth(trList, period)
        val smoothedPlusDM = smooth(plusDmList, period)
        val smoothedMinusDM = smooth(minusDmList, period)

        if (smoothedTR.isEmpty()) return null

        val dxList = mutableListOf<BigDecimal>()
        var lastPlusDI = BigDecimal.ZERO
        var lastMinusDI = BigDecimal.ZERO

        val minLen = minOf(smoothedTR.size, smoothedPlusDM.size, smoothedMinusDM.size)

        for (i in 0 until minLen) {
            val trVal = smoothedTR[i]
            if (trVal.compareTo(BigDecimal.ZERO) == 0) continue

            val pDI = smoothedPlusDM[i].divide(trVal, 8, RoundingMode.HALF_UP).multiply(BigDecimal(100))
            val mDI = smoothedMinusDM[i].divide(trVal, 8, RoundingMode.HALF_UP).multiply(BigDecimal(100))

            lastPlusDI = pDI
            lastMinusDI = mDI

            val diSum = pDI.add(mDI)
            if (diSum > BigDecimal.ZERO) {
                val dx = pDI.subtract(mDI).abs().divide(diSum, 8, RoundingMode.HALF_UP).multiply(BigDecimal(100))
                dxList.add(dx)
            }
        }

        val adxList = smooth(dxList, period)
        val lastADX = if (adxList.isNotEmpty()) adxList.last() else BigDecimal.ZERO

        return Triple(lastADX, lastPlusDI, lastMinusDI)
    }

    // --- ALLIGATOR (Williams Alligator) ---
    // Dönüş: Triple(Jaw, Teeth, Lips)
    fun calculateAlligator(prices: List<BigDecimal>): Triple<BigDecimal, BigDecimal, BigDecimal>? {
        if (prices.size < 25) return null

        fun calculateSMMA(data: List<BigDecimal>, period: Int): List<BigDecimal> {
            val smmaList = mutableListOf<BigDecimal>()
            if (data.size < period) return smmaList

            var sum = BigDecimal.ZERO
            for (i in 0 until period) sum = sum.add(data[i])
            var prev = sum.divide(BigDecimal(period), 8, RoundingMode.HALF_UP)
            smmaList.add(prev)

            for (i in period until data.size) {
                val nextVal = prev.multiply(BigDecimal(period - 1)).add(data[i]).divide(BigDecimal(period), 8, RoundingMode.HALF_UP)
                smmaList.add(nextVal)
                prev = nextVal
            }
            return smmaList
        }

        val jawSeries = calculateSMMA(prices, 13)
        val teethSeries = calculateSMMA(prices, 8)
        val lipsSeries = calculateSMMA(prices, 5)

        // Shift (Öteleme) mantığı: Bugünkü değer aslında X gün önceki hesaplanan değerdir.
        if (jawSeries.size < 9 || teethSeries.size < 6 || lipsSeries.size < 4) return null

        val currentJaw = jawSeries[jawSeries.size - 1 - 8]
        val currentTeeth = teethSeries[teethSeries.size - 1 - 5]
        val currentLips = lipsSeries[lipsSeries.size - 1 - 3]

        return Triple(currentJaw, currentTeeth, currentLips)
    }

    // --- AROON ---
    fun calculateAroon(highs: List<BigDecimal>, lows: List<BigDecimal>, period: Int = 14): Pair<BigDecimal, BigDecimal>? {
        if (highs.size < period + 1) return null

        val recentHighs = highs.takeLast(period + 1)
        val recentLows = lows.takeLast(period + 1)

        var highIndex = 0
        var lowIndex = 0
        var maxVal = BigDecimal("-999999999")
        var minVal = BigDecimal("999999999")

        for (i in recentHighs.indices) {
            if (recentHighs[i] > maxVal) { maxVal = recentHighs[i]; highIndex = i }
            if (recentLows[i] < minVal) { minVal = recentLows[i]; lowIndex = i }
        }

        // Index 0 en eski, Index period en yeni (bugün).
        // Gün sayısı = (Toplam Eleman - 1) - Bulunan Index
        val daysSinceHigh = period - highIndex
        val daysSinceLow = period - lowIndex

        val aroonUp = BigDecimal(period - daysSinceHigh).divide(BigDecimal(period), 8, RoundingMode.HALF_UP).multiply(BigDecimal(100))
        val aroonDown = BigDecimal(period - daysSinceLow).divide(BigDecimal(period), 8, RoundingMode.HALF_UP).multiply(BigDecimal(100))

        return Pair(aroonUp, aroonDown)
    }

    // --- CMO (Chande Momentum Oscillator) ---
    fun calculateCMO(prices: List<BigDecimal>, period: Int = 9): BigDecimal? {
        if (prices.size < period + 1) return null

        var sumGain = BigDecimal.ZERO
        var sumLoss = BigDecimal.ZERO
        val relevantPrices = prices.takeLast(period + 1)

        for (i in 1 until relevantPrices.size) {
            val diff = relevantPrices[i].subtract(relevantPrices[i - 1])
            if (diff > BigDecimal.ZERO) sumGain = sumGain.add(diff)
            else sumLoss = sumLoss.add(diff.abs())
        }

        val totalMovement = sumGain.add(sumLoss)
        if (totalMovement.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO

        return sumGain.subtract(sumLoss)
            .divide(totalMovement, 8, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
    }

    // --- MFI (Money Flow Index) ---
    fun calculateMFI(highs: List<BigDecimal>, lows: List<BigDecimal>, closes: List<BigDecimal>, volumes: List<BigDecimal>, period: Int = 14): BigDecimal? {
        if (closes.size < period + 1) return null

        var posFlow = BigDecimal.ZERO
        var negFlow = BigDecimal.ZERO

        // Son 'period' kadar muma bak
        for (i in closes.size - period until closes.size) {
            val tp = (highs[i].add(lows[i]).add(closes[i])).divide(BigDecimal(3), 8, RoundingMode.HALF_UP)
            val prevTp = (highs[i-1].add(lows[i-1]).add(closes[i-1])).divide(BigDecimal(3), 8, RoundingMode.HALF_UP)
            val rawFlow = tp.multiply(volumes[i])

            if (tp > prevTp) posFlow = posFlow.add(rawFlow)
            else if (tp < prevTp) negFlow = negFlow.add(rawFlow)
        }

        if (negFlow.compareTo(BigDecimal.ZERO) == 0) return BigDecimal(100)
        val mfiRatio = posFlow.divide(negFlow, 8, RoundingMode.HALF_UP)
        return BigDecimal(100).subtract(BigDecimal(100).divide(BigDecimal.ONE.add(mfiRatio), 8, RoundingMode.HALF_UP))
    }

    // --- CMF (Chaikin Money Flow) ---
    fun calculateCMF(highs: List<BigDecimal>, lows: List<BigDecimal>, closes: List<BigDecimal>, volumes: List<BigDecimal>, period: Int = 20): BigDecimal? {
        if (closes.size < period) return null

        var adSum = BigDecimal.ZERO
        var volSum = BigDecimal.ZERO

        val start = closes.size - period
        for (i in start until closes.size) {
            val h = highs[i]
            val l = lows[i]
            val c = closes[i]
            val v = volumes[i]

            val range = h.subtract(l)
            if (range.compareTo(BigDecimal.ZERO) == 0) continue

            // ((Close - Low) - (High - Close)) / (High - Low)
            val multiplier = (c.subtract(l).subtract(h.subtract(c))).divide(range, 8, RoundingMode.HALF_UP)
            val ad = multiplier.multiply(v)

            adSum = adSum.add(ad)
            volSum = volSum.add(v)
        }

        if (volSum.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        return adSum.divide(volSum, 8, RoundingMode.HALF_UP)
    }

    // --- OBV (On Balance Volume) ---
    // Basit trend için son değer ile önceki MA karşılaştırması
    fun calculateOBV(closes: List<BigDecimal>, volumes: List<BigDecimal>): Pair<BigDecimal, BigDecimal>? {
        if (closes.isEmpty()) return null

        // Tüm geçmiş için OBV hesapla
        val obvList = mutableListOf<BigDecimal>()
        var currentObv = BigDecimal.ZERO
        obvList.add(currentObv)

        for (i in 1 until closes.size) {
            val current = closes[i]
            val prev = closes[i-1]
            val vol = volumes[i]

            if (current > prev) currentObv = currentObv.add(vol)
            else if (current < prev) currentObv = currentObv.subtract(vol)
            obvList.add(currentObv)
        }

        val lastObv = obvList.last()
        // Son 20 mumun ortalaması (Signal Line gibi)
        if (obvList.size < 20) return Pair(lastObv, lastObv)

        var sum = BigDecimal.ZERO
        for (i in obvList.size - 20 until obvList.size) {
            sum = sum.add(obvList[i])
        }
        val obvMa = sum.divide(BigDecimal(20), 8, RoundingMode.HALF_UP)

        return Pair(lastObv, obvMa)
    }

    // --- ATR (Average True Range) - Volatilite Ölçer ---
    fun calculateATR(highs: List<BigDecimal>, lows: List<BigDecimal>, closes: List<BigDecimal>, period: Int = 14): BigDecimal? {
        if (highs.size < period + 1) return null

        val trList = mutableListOf<BigDecimal>()
        for (i in 1 until highs.size) {
            val h = highs[i]
            val l = lows[i]
            val pc = closes[i - 1]

            val tr1 = h.subtract(l)
            val tr2 = h.subtract(pc).abs()
            val tr3 = l.subtract(pc).abs()

            trList.add(tr1.max(tr2).max(tr3))
        }

        // Basit Hareketli Ortalama (SMA) ile ATR hesapla
        if (trList.size < period) return null

        // Son 'period' kadar olan TR'lerin ortalaması
        val recentTRs = trList.takeLast(period)
        var sum = BigDecimal.ZERO
        for (tr in recentTRs) sum = sum.add(tr)

        return sum.divide(BigDecimal(period), 8, RoundingMode.HALF_UP)
    }
}