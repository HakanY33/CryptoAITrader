package com.example.cryptopredictionapp.data.repository

import com.example.cryptopredictionapp.data.api.RetrofitClient
import com.example.cryptopredictionapp.data.api.AiRetrofitClient
import com.example.cryptopredictionapp.data.model.BingxKlineData
import com.example.cryptopredictionapp.data.model.BingxTickerData
import com.example.cryptopredictionapp.data.model.MarketDataRequest

class CryptoRepository {

    // 1. Anlık Fiyat
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

    // 2. Teknik Analiz için Ham Mumlar (OB/FVG)
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

    // 3. Yapay Zeka Sorgusu
    suspend fun askAiForAnalysis(request: MarketDataRequest): String {
        return try {
            val response = AiRetrofitClient.api.askGemini(request)
            response.message
        } catch (e: Exception) {
            e.printStackTrace()
            "AI Bağlantı Hatası: ${e.localizedMessage}. Bilgisayarın IP'sini ve Firewall ayarlarını kontrol et."
        }
    }

    // 4. YENİ: Tüm Coin Listesini Getir
    suspend fun getAllSymbols(): List<String> {
        return try {
            val response = RetrofitClient.api.getContracts()
            if (response.code == 0 && !response.data.isNullOrEmpty()) {
                response.data
                    .map { it.symbol } // Sadece isimleri al
                    .sorted()          // Alfabetik sırala
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}