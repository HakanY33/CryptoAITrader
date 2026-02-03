package com.example.cryptopredictionapp.data.model

import com.google.gson.annotations.SerializedName

// Sadece Tarayıcı (Scanner) için kullanılacak özel model.
// Ana ekranın modeliyle karışmaz, hata yapma riskini sıfırlar.
data class BingxMarketResponse(
    @SerializedName("code") val code: Int?,
    @SerializedName("data") val data: List<BingxMarketItem>?
)

data class BingxMarketItem(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("lastPrice") val lastPrice: String?,
    @SerializedName("priceChangePercent") val priceChangePercent: String?, // Yüzdelik değişim
    @SerializedName("quoteVolume") val quoteVolume: String? // Hacim
)