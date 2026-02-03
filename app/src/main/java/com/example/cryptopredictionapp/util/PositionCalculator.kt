package com.example.cryptopredictionapp.util

import java.math.BigDecimal
import java.math.RoundingMode

object PositionCalculator {

    // 1. İşlem Büyüklüğü Hesaplama
    fun calculatePositionSize(
        walletBalance: BigDecimal, // Kasa (Ör: 1000$)
        riskPercentage: BigDecimal, // Risk Yüzdesi (Ör: 0.01 yani %1)
        leverage: Int // Kaldıraç (Ör: 100)
    ): BigDecimal {
        // Marj = Kasa * %1 -> 1000 * 0.01 = 10$
        val margin = walletBalance.multiply(riskPercentage)
        // Kontrat Büyüklüğü = Marj * Kaldıraç -> 10 * 100 = 1000$
        return margin.multiply(BigDecimal(leverage))
    }

    // 2. Ekleme Yapınca Yeni Ortalama (Entry Price) Hesaplama
    // Formül: (EskiMaliyet * EskiAdet + YeniMaliyet * YeniAdet) / ToplamAdet
    fun calculateNewEntryPrice(
        currentEntryPrice: BigDecimal,
        currentSize: BigDecimal, // Mevcut işlem büyüklüğü ($)
        addedEntryPrice: BigDecimal, // Ekleme yaptığımız yerin fiyatı
        addedSize: BigDecimal // Eklenen miktar ($)
    ): BigDecimal {

        // İşlemde kaç adet coin var? (Size / Price)
        val currentCoinAmount = currentSize.divide(currentEntryPrice, 8, RoundingMode.HALF_UP)
        val addedCoinAmount = addedSize.divide(addedEntryPrice, 8, RoundingMode.HALF_UP)

        val totalSize = currentSize.add(addedSize)
        val totalCoins = currentCoinAmount.add(addedCoinAmount)

        // Yeni Ortalama = Toplam Para / Toplam Coin Adedi
        return totalSize.divide(totalCoins, 8, RoundingMode.HALF_UP)
    }

    // 3. Tahmini Zarar Hesaplama (%500 geriye düştü senaryosu için)
    fun calculatePnLPercentage(
        entryPrice: BigDecimal,
        currentPrice: BigDecimal,
        leverage: Int,
        isLong: Boolean
    ): BigDecimal {
        val priceDiff = if (isLong) {
            currentPrice.subtract(entryPrice)
        } else {
            entryPrice.subtract(currentPrice)
        }

        // Yüzde Değişim = (Fark / Giriş) * 100 * Kaldıraç
        return priceDiff.divide(entryPrice, 8, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .multiply(BigDecimal(leverage))
    }
}