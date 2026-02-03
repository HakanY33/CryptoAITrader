package com.example.cryptopredictionapp.data.model

import com.google.gson.annotations.SerializedName

// Python'a göndereceğimiz paket (Sırt çantamız)
data class MarketDataRequest(
    val symbol: String,
    val price: String,
    val ema21: String,
    val ema50: String,
    val trend: String,
    @SerializedName("ob_status") val obStatus: String,   // Python'da ob_status dedik
    @SerializedName("fvg_status") val fvgStatus: String  // Python'da fvg_status dedik
)

// Python'dan (Gemini'den) gelecek cevap
data class AiResponse(
    @SerializedName("ai_response")
    val message: String
)