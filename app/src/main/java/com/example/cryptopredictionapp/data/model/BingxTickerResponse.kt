package com.example.cryptopredictionapp.data.model

import com.google.gson.annotations.SerializedName

data class BingxTickerResponse(
    val code: Int?,
    val msg: String?,
    val data: BingxTickerData?
)

data class BingxTickerData(
    @SerializedName("symbol")
    val symbol: String?,

    @SerializedName("lastPrice")
    val lastPrice: String?,

    @SerializedName("highPrice")
    val highPrice: String?,

    @SerializedName("lowPrice")
    val lowPrice: String?,

    @SerializedName("volume")
    val volume: String?
)