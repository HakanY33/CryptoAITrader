package com.example.cryptopredictionapp.data.model

import com.google.gson.annotations.SerializedName

data class BingxTradeResponse(
    @SerializedName("code") val code: Int, // 0 ise Başarılı
    @SerializedName("msg") val msg: String?, // Hata mesajı varsa
    @SerializedName("data") val data: Any? // İşlem detayı
)