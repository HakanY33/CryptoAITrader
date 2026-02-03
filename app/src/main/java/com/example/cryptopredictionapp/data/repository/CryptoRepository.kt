package com.example.cryptopredictionapp.data.repository

import com.example.cryptopredictionapp.data.api.RetrofitClient
import com.example.cryptopredictionapp.data.api.AiRetrofitClient
import com.example.cryptopredictionapp.data.model.BingxKlineData
import com.example.cryptopredictionapp.data.model.BingxTickerData
import com.example.cryptopredictionapp.data.model.BingxMarketItem
import com.example.cryptopredictionapp.data.model.MarketDataRequest

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

    // 5. MARKET TARAMA (AVCI)
    suspend fun fetchTopMovers(): Map<String, List<BingxMarketItem>> {
        return try {
            val response = RetrofitClient.api.getMarketScanData()
            if (response.code == 0 && !response.data.isNullOrEmpty()) {
                val usdtPairs = response.data.filter { it.symbol.endsWith("-USDT") }
                val gainers = usdtPairs.sortedByDescending { it.priceChangePercent?.toDoubleOrNull() ?: -999.0 }.take(15)
                val losers = usdtPairs.sortedBy { it.priceChangePercent?.toDoubleOrNull() ?: 999.0 }.take(15)
                mapOf("GAINERS" to gainers, "LOSERS" to losers)
            } else { emptyMap() }
        } catch (e: Exception) { emptyMap() }
    }
}