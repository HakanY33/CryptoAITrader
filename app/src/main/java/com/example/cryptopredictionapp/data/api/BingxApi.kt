package com.example.cryptopredictionapp.data.api

import com.example.cryptopredictionapp.data.model.BingxContractResponse
import com.example.cryptopredictionapp.data.model.BingxKlineResponse
import com.example.cryptopredictionapp.data.model.BingxTickerResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BingxApi {

    @GET("openApi/swap/v2/quote/ticker")
    suspend fun getTicker(
        @Query("symbol") symbol: String
    ): BingxTickerResponse

    @GET("openApi/swap/v3/quote/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 1000
    ): BingxKlineResponse

    // YENİ: Tüm coin listesini çeken uç nokta
    @GET("openApi/swap/v2/quote/contracts")
    suspend fun getContracts(): BingxContractResponse
}