package com.example.cryptopredictionapp.data.model

import com.google.gson.annotations.SerializedName

data class BingxBalanceResponse(
    val code: Int,
    val msg: String?,
    val data: BalanceData?
)

data class BalanceData(
    val balance: BalanceDetail?
)

data class BalanceDetail(
    @SerializedName("equity") val equity: String?,
    @SerializedName("balance") val balance: String?,
    @SerializedName("availableMargin") val availableMargin: String?
)