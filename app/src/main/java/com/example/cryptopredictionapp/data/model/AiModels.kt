package com.example.cryptopredictionapp.data.model

import com.google.gson.annotations.SerializedName

// Python'a göndereceğimiz paket (Sırt çantamız)
data class MarketDataRequest(
    val symbol: String,
    val price: String,
    val trend: String,     // EMA 21/50 yerine artık Trend metnini yolluyoruz
    @SerializedName("rsi_status") val rsiStatus: String, // Yeni
    @SerializedName("ob_status") val obStatus: String,
    @SerializedName("fvg_status") val fvgStatus: String,
    @SerializedName("setup_entry") val setupEntry: String, // Yeni
    @SerializedName("setup_tp") val setupTp: String,       // Yeni
    @SerializedName("setup_sl") val setupSl: String        // Yeni
)

// Python'dan (Gemini'den) gelecek cevap
data class AiResponse(
    @SerializedName("ai_response")
    val message: String
)