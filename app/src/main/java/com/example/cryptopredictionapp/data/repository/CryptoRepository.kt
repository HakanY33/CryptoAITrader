package com.example.cryptopredictionapp.data.repository

import com.example.cryptopredictionapp.data.api.RetrofitClient
import com.example.cryptopredictionapp.data.api.AiRetrofitClient
import com.example.cryptopredictionapp.data.model.BingxKlineData
import com.example.cryptopredictionapp.data.model.BingxTickerData
import com.example.cryptopredictionapp.data.model.BingxMarketItem
import com.example.cryptopredictionapp.data.model.MarketDataRequest
import java.util.TreeMap
import com.example.cryptopredictionapp.util.Constants
import com.example.cryptopredictionapp.util.BingxSignatureUtils
import java.math.BigDecimal
import java.math.RoundingMode


class CryptoRepository {

    // 1. ANLIK FİYAT
    suspend fun getCryptoPrice(symbol: String): BingxTickerData? {
        return try {
            val response = RetrofitClient.api.getTicker(symbol)
            if (response.code == 0 && response.data != null) {
                response.data
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    // Elvis Operatörü (?:) eklendi. Null gelirse "..." döner.
    suspend fun getMarketPrice(symbol: String): String {
        return try {
            val response = RetrofitClient.api.getTicker(symbol)
            if (response.code == 0 && response.data != null) {
                response.data.lastPrice ?: "..."
            } else {
                "..."
            }
        } catch (e: Exception) {
            "..."
        }
    }

    // 2. MUM VERİLERİ
    suspend fun getKlinesData(symbol: String, interval: String): List<BingxKlineData> {
        return try {
            val response = RetrofitClient.api.getKlines(symbol, interval, 1000)
            if (response.code == 0 && !response.data.isNullOrEmpty()) {
                response.data
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 3. AI SORGUSU
    suspend fun askAiForAnalysis(request: MarketDataRequest): String {
        return try {
            val response = AiRetrofitClient.api.askGemini(request)
            response.message
        } catch (e: Exception) {
            "AI Bağlantı Hatası: ${e.localizedMessage}"
        }
    }

    // 4. SEMBOL LİSTESİ
    suspend fun getAllSymbols(): List<String> {
        return try {
            val response = RetrofitClient.api.getContracts()
            if (response.code == 0 && !response.data.isNullOrEmpty()) {
                response.data.map { it.symbol }.sorted()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 5. MARKET TARAMA (Limit 20 oldu)
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

    // 6. BAKİYE SORGUSU
    suspend fun getAccountBalance(): Double {
        return try {
            val timestamp = System.currentTimeMillis()
            val params = TreeMap<String, String>()
            params["apiKey"] = Constants.API_KEY
            params["timestamp"] = timestamp.toString()

            val queryString = BingxSignatureUtils.createQueryString(params)
            val signature = BingxSignatureUtils.generateSignature(queryString, Constants.SECRET_KEY)
            val fullUrl = "${Constants.BASE_URL}openApi/swap/v2/user/balance?$queryString&signature=$signature"

            val response = RetrofitClient.api.getBalance(Constants.API_KEY, fullUrl)

            if (response.code == 0) {
                val money = response.data?.balance?.equity ?: response.data?.balance?.balance
                money?.toDoubleOrNull() ?: 0.0
            } else {
                0.0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0.0
        }
    }

    // TP/SL HASSASİYETİ İÇİN YARDIMCI FONKSİYON
    private fun getPrecision(value: Double): Int {
        val text = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
        val index = text.indexOf('.')
        return if (index < 0) 0 else text.length - index - 1
    }

    // 7. AKILLI İŞLEM AÇMA (RETRY YOK - TEK ATIŞ - HASSASİYET DÜZELTİLMİŞ)
    suspend fun placeSmartTrade(
        symbol: String,
        side: String,
        price: Double,
        leverage: Int,
        tpPrice: Double = 0.0,
        slPrice: Double = 0.0
    ): String {
        return try { // While döngüsü kaldırıldı, sadece Try-Catch var
            val currentBalance = getAccountBalance()
            if (currentBalance <= 5.0) return "❌ Bakiye Yetersiz"

            val marginAmount = currentBalance * 0.01 // %1 Risk
            val tradeValueUSDT = marginAmount * leverage
            val rawQuantity = tradeValueUSDT / price

            // Miktar Hassasiyeti (Altcoinler için tamsayı)
            val qtyScale = if (price < 10.0) 0 else 4
            val quantity = BigDecimal.valueOf(rawQuantity)
                .setScale(qtyScale, RoundingMode.DOWN)
                .toPlainString()

            if (quantity == "0") return "❌ Miktar Çok Düşük!"

            // Fiyat Hassasiyeti (TP/SL için Kritik Düzeltme)
            val pricePrecision = getPrecision(price)

            val timestamp = System.currentTimeMillis()
            val params = TreeMap<String, String>()
            params["apiKey"] = Constants.API_KEY
            params["type"] = "MARKET"
            params["positionSide"] = if (side == "BUY") "LONG" else "SHORT"
            params["side"] = side
            params["symbol"] = symbol
            params["quantity"] = quantity
            params["timestamp"] = timestamp.toString()
            params["leverage"] = leverage.toString()

            // TP/SL Ekleme (Hassasiyete Göre Formatlanmış)
            if (tpPrice > 0.0) {
                val formattedTp = BigDecimal.valueOf(tpPrice)
                    .setScale(pricePrecision, RoundingMode.HALF_UP)
                    .toPlainString()
                params["takeProfitPrice"] = formattedTp
                println("DEBUG: TP Gönderiliyor: $formattedTp (Basamak: $pricePrecision)")
            }

            if (slPrice > 0.0) {
                val formattedSl = BigDecimal.valueOf(slPrice)
                    .setScale(pricePrecision, RoundingMode.HALF_UP)
                    .toPlainString()
                params["stopLossPrice"] = formattedSl
                println("DEBUG: SL Gönderiliyor: $formattedSl")
            }

            val queryString = BingxSignatureUtils.createQueryString(params)
            val signature = BingxSignatureUtils.generateSignature(queryString, Constants.SECRET_KEY)
            val fullUrl = "${Constants.BASE_URL}openApi/swap/v2/trade/order?$queryString&signature=$signature"

            val response = RetrofitClient.api.placeOrder(Constants.API_KEY, fullUrl)

            if (response.code == 0) {
                "✅ İŞLEM AÇILDI!\nCoin: $symbol\nAdet: $quantity"
            } else {
                "❌ BingX Hatası (${response.code}):\n${response.msg}"
            }

        } catch (e: Exception) {
            "Hata: ${e.message}"
        }
    }
}