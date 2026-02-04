package com.example.cryptopredictionapp.util

import java.math.BigDecimal
import java.math.RoundingMode

object PositionCalculator {

    // --- ESKİ METOTLAR (Dursun, belki lazım olur) ---
    fun calculatePositionSize(walletBalance: BigDecimal, riskPercentage: BigDecimal, leverage: Int): BigDecimal {
        val margin = walletBalance.multiply(riskPercentage)
        return margin.multiply(BigDecimal(leverage))
    }

    // --- YENİ: PROFESYONEL RİSK YÖNETİMİ (Money Printer Stratejisi) ---
    // Formül: Quantity = (Kasa * Risk%) / |Giriş - StopLoss|
    // Bu yöntem kaldıraçtan bağımsızdır. Stop olduğunda hep aynı $ miktarını kaybedersiniz.
    fun calculateRiskBasedSize(
        walletBalance: BigDecimal, // Toplam Kasa (Örn: 1000$)
        riskPercentage: BigDecimal, // İşlem Başına Risk (Örn: 0.01 yani %1)
        entryPrice: BigDecimal,
        stopLossPrice: BigDecimal
    ): BigDecimal {

        // 1. Riske atılacak tutarı bul (Örn: 1000$ * %1 = 10$)
        val riskAmount = walletBalance.multiply(riskPercentage)

        // 2. Stop mesafesini bul (Fiyat Farkı)
        val stopDistance = entryPrice.subtract(stopLossPrice).abs()

        // Sıfıra bölme hatasını önle
        if (stopDistance.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO

        // 3. Alınması gereken Coin Adedi (Size)
        // Örn: Stop mesafesi 100$ ise ve 10$ risk ediyorsak -> 0.1 BTC almalıyız.
        return riskAmount.divide(stopDistance, 6, RoundingMode.HALF_UP)
    }

    // --- PnL Hesaplama (Aynı Kalabilir) ---
    fun calculatePnLPercentage(entryPrice: BigDecimal, currentPrice: BigDecimal, leverage: Int, isLong: Boolean): BigDecimal {
        val priceDiff = if (isLong) currentPrice.subtract(entryPrice) else entryPrice.subtract(currentPrice)
        if (entryPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO

        return priceDiff.divide(entryPrice, 8, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .multiply(BigDecimal(leverage))
    }
}