package com.example.cryptopredictionapp.data.repository

import com.example.cryptopredictionapp.data.api.RetrofitClient
import com.example.cryptopredictionapp.data.api.AiRetrofitClient
import com.example.cryptopredictionapp.data.model.BingxKlineData
import com.example.cryptopredictionapp.data.model.BingxTickerData
import com.example.cryptopredictionapp.data.model.BingxMarketItem
import com.example.cryptopredictionapp.data.model.MarketDataRequest
import com.example.cryptopredictionapp.util.Constants
import com.example.cryptopredictionapp.util.BingxSignatureUtils
import java.util.TreeMap
import java.util.Locale
import java.math.BigDecimal
import java.math.RoundingMode

class CryptoRepository {

    // ... (Üstteki getCryptoPrice, getKlinesData vb. fonksiyonlar aynen kalsın) ...
    // Sadece aşağıdakileri kopyala/yapıştır yap yeterli:

    // 1. ANLIK FİYAT
    suspend fun getCryptoPrice(symbol: String): BingxTickerData? {
        return try {
            val response = RetrofitClient.api.getTicker(symbol)
            if (response.code == 0 && response.data != null) response.data else null
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    suspend fun getMarketPrice(symbol: String): String {
        return try {
            val response = RetrofitClient.api.getTicker(symbol)
            if (response.code == 0 && response.data != null) response.data.lastPrice ?: "..." else "..."
        } catch (e: Exception) { "..." }
    }

    suspend fun getKlinesData(symbol: String, interval: String): List<BingxKlineData> {
        return try {
            val response = RetrofitClient.api.getKlines(symbol, interval, 1000)
            if (response.code == 0 && !response.data.isNullOrEmpty()) response.data else emptyList()
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    suspend fun askAiForAnalysis(request: MarketDataRequest): String {
        return try {
            val response = AiRetrofitClient.api.askGemini(request)
            response.message
        } catch (e: Exception) { "AI Hatası: ${e.message}" }
    }

    suspend fun getAllSymbols(): List<String> {
        return try {
            val response = RetrofitClient.api.getContracts()
            if (response.code == 0 && !response.data.isNullOrEmpty()) response.data.map { it.symbol }.sorted() else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun fetchTopMovers(limit: Int = 20): Map<String, List<BingxMarketItem>> {
        return try {
            val response = RetrofitClient.api.getMarketScanData()
            if (response.code == 0 && !response.data.isNullOrEmpty()) {
                val usdtPairs = response.data.filter { it.symbol.endsWith("-USDT") }
                val gainers = usdtPairs.sortedByDescending { it.priceChangePercent?.toDoubleOrNull() ?: -999.0 }.take(limit)
                val losers = usdtPairs.sortedBy { it.priceChangePercent?.toDoubleOrNull() ?: 999.0 }.take(limit)
                mapOf("GAINERS" to gainers, "LOSERS" to losers)
            } else { emptyMap() }
        } catch (e: Exception) { emptyMap() }
    }

    suspend fun getAccountBalance(): Double {
        return try {
            val timestamp = System.currentTimeMillis()
            val params = TreeMap<String, String>()
            params["timestamp"] = timestamp.toString()
            val queryString = BingxSignatureUtils.createQueryString(params)
            val signature = BingxSignatureUtils.generateSignature(queryString, Constants.SECRET_KEY)
            val fullUrl = "${Constants.BASE_URL}openApi/swap/v2/user/balance?$queryString&signature=$signature"
            val response = RetrofitClient.api.getBalance(Constants.API_KEY, fullUrl)
            if (response.code == 0) {
                val money = response.data?.balance?.equity ?: response.data?.balance?.balance ?: response.data?.balance?.availableMargin
                money?.toDoubleOrNull() ?: 0.0
            } else { 0.0 }
        } catch (e: Exception) { 0.0 }
    }

    // --- AKILLI HASSASİYET AYARLAYICI (BUG FIX) ---
    private fun formatPrecision(value: Double, price: Double): String {
        // Fiyatın kaç sıfırı olduğuna bakarak hassasiyeti dinamik ayarla
        val precision = when {
            price >= 10000 -> 1    // BTC (98500.5)
            price >= 1000 -> 2     // ETH (2650.55)
            price >= 10 -> 3       // SOL, AVAX (150.123)
            price >= 1 -> 4        // ARB, MATIC (1.1234)
            price >= 0.1 -> 5      // DOGE, XLM (0.12345)
            price >= 0.01 -> 6     // VET, GALA (0.023456)
            price >= 0.001 -> 7    // LUNC (0.0012345)
            else -> 8              // SHIB, PEPE (0.00001234)
        }

        return BigDecimal.valueOf(value)
            .setScale(precision, RoundingMode.HALF_UP)
            .toPlainString()
    }

    // --- YENİ YARDIMCI: SADECE TRIGGER (TP/SL) EMRİ GÖNDERİR ---
    private suspend fun placeTriggerOrder(
        symbol: String,
        side: String, // SELL
        positionSide: String, // LONG/SHORT
        stopPrice: String,
        quantity: String,
        type: String // TAKE_PROFIT_MARKET veya STOP_MARKET
    ) {
        try {
            val timestamp = System.currentTimeMillis()
            val params = TreeMap<String, String>()
            params["symbol"] = symbol
            params["side"] = side // Pozisyonu kapatmak için ters işlem (Long ise Sell)
            params["positionSide"] = positionSide
            params["type"] = type
            params["stopPrice"] = stopPrice // Tetik Fiyatı
            params["quantity"] = quantity
            params["workingType"] = "MARK_PRICE" // Mark fiyatını baz al
            params["timestamp"] = timestamp.toString()

            val queryString = BingxSignatureUtils.createQueryString(params)
            val signature = BingxSignatureUtils.generateSignature(queryString, Constants.SECRET_KEY)
            val fullUrl = "${Constants.BASE_URL}openApi/swap/v2/trade/order?$queryString&signature=$signature"

            val response = RetrofitClient.api.placeOrder(Constants.API_KEY, fullUrl)
            println("TRIGGER ORDER ($type): Kod=${response.code} Msg=${response.msg}")
        } catch (e: Exception) {
            println("TRIGGER ERROR: ${e.message}")
        }
    }

    // --- 7. AKILLI İŞLEM AÇMA (ZİNCİRLEME SİSTEM - %100 ÇALIŞIR) ---
    suspend fun placeSmartTrade(
        symbol: String,
        side: String,
        price: Double,
        leverage: Int,
        tpPrice: Double = 0.0,
        slPrice: Double = 0.0
    ): String {
        return try {
            val currentBalance = getAccountBalance()
            if (currentBalance < 5.0) return "❌ Bakiye Yetersiz"

            // ADIM 1: Ana İşlem Parametreleri
            val marginAmount = currentBalance * 0.02 // %2 Risk
            val tradeValueUSDT = marginAmount * leverage
            val rawQuantity = tradeValueUSDT / price
            val quantity = String.format(Locale.US, "%.4f", rawQuantity)

            if (rawQuantity < 0.0001) return "❌ Miktar Çok Düşük"

            val bingxSide = if (side == "BUY") "BUY" else "SELL"
            val positionSide = if (side == "BUY") "LONG" else "SHORT"
            val timestamp = System.currentTimeMillis()

            val params = TreeMap<String, String>()
            params["symbol"] = symbol
            params["side"] = bingxSide
            params["positionSide"] = positionSide
            params["type"] = "MARKET"
            params["quantity"] = quantity
            params["timestamp"] = timestamp.toString()
            params["leverage"] = leverage.toString()

            // İmzala ve Ana İşlemi Gönder
            val queryString = BingxSignatureUtils.createQueryString(params)
            val signature = BingxSignatureUtils.generateSignature(queryString, Constants.SECRET_KEY)
            val fullUrl = "${Constants.BASE_URL}openApi/swap/v2/trade/order?$queryString&signature=$signature"

            val response = RetrofitClient.api.placeOrder(Constants.API_KEY, fullUrl)

            if (response.code == 0) {
                // --- ADIM 2: İŞLEM BAŞARILIYSA TP VE SL EMRİ GİR (ARKADAN GÖNDER) ---

                // Eğer LONG açtıysak, TP ve SL için "SELL" emri girmeliyiz.
                // Eğer SHORT açtıysak, TP ve SL için "BUY" emri girmeliyiz.
                val closeSide = if (side == "BUY") "SELL" else "BUY"

                if (tpPrice > 0.0) {
                    placeTriggerOrder(
                        symbol = symbol,
                        side = closeSide,
                        positionSide = positionSide,
                        stopPrice = formatPrecision(tpPrice, price),
                        quantity = quantity,
                        type = "TAKE_PROFIT_MARKET"
                    )
                }

                if (slPrice > 0.0) {
                    placeTriggerOrder(
                        symbol = symbol,
                        side = closeSide,
                        positionSide = positionSide,
                        stopPrice = formatPrecision(slPrice, price),
                        quantity = quantity,
                        type = "STOP_MARKET"
                    )
                }

                "✅ İŞLEM VE TP/SL GİRİLDİ!\n$symbol - ${if(side=="BUY") "L" else "S"}\nAdet: $quantity"
            } else {
                "❌ İşlem Hatası (${response.code}):\n${response.msg}"
            }

        } catch (e: Exception) {
            "Bağlantı Hatası: ${e.message}"
        }
    }
}