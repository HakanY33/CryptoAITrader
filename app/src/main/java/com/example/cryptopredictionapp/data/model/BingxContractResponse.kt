package com.example.cryptopredictionapp.data.model

import com.google.gson.annotations.SerializedName

// BingX'ten gelen Coin Listesi Cevabı
data class BingxContractResponse(
    val code: Int?,
    val data: List<BingxContractData>?
)

data class BingxContractData(
    val symbol: String, // Örn: BTC-USDT
    val status: Int?    // 1: Açık, 0: Kapalı
)