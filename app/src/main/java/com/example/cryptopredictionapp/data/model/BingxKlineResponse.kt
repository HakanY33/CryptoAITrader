package com.example.cryptopredictionapp.data.model

import com.google.gson.annotations.SerializedName

// API'den dönen ana cevap
data class BingxKlineResponse(
    val code: Int?,
    val msg: String?,
    val data: List<BingxKlineData>? // Bu sefer kesinlikle liste gelecek (Geçmiş mumlar)
)

// Tek bir mumun verisi
data class BingxKlineData(
    @SerializedName("time")
    val time: Long, // Mumun saati (Milisaniye cinsinden)

    @SerializedName("open")
    val open: String, // Açılış Fiyatı

    @SerializedName("high")
    val high: String, // En Yüksek

    @SerializedName("low")
    val low: String, // En Düşük

    @SerializedName("close")
    val close: String, // Kapanış (EMA hesabı için en önemlisi bu)

    @SerializedName("volume")
    val volume: String // Hacim
)