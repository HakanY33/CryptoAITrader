package com.example.cryptopredictionapp.data.api

import com.example.cryptopredictionapp.data.model.BingxBalanceResponse // YENİ
import com.example.cryptopredictionapp.data.model.BingxContractResponse
import com.example.cryptopredictionapp.data.model.BingxKlineResponse
import com.example.cryptopredictionapp.data.model.BingxMarketResponse
import com.example.cryptopredictionapp.data.model.BingxTickerResponse
import com.example.cryptopredictionapp.data.model.BingxTradeResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface BingxApi {
    @GET("openApi/swap/v2/quote/ticker")
    suspend fun getTicker(@Query("symbol") symbol: String): BingxTickerResponse

    @GET("openApi/swap/v3/quote/klines")
    suspend fun getKlines(@Query("symbol") symbol: String, @Query("interval") interval: String, @Query("limit") limit: Int = 1000): BingxKlineResponse

    @GET("openApi/swap/v2/quote/contracts")
    suspend fun getContracts(): BingxContractResponse

    @GET("openApi/swap/v2/quote/ticker")
    suspend fun getAllTickers(): BingxTickerResponse

    @GET("openApi/swap/v2/quote/ticker")
    suspend fun getMarketScanData(): BingxMarketResponse


    @GET
    suspend fun getBalance(
        @Header("X-BX-APIKEY") apiKey: String,
        @Url fullUrl: String
    ): BingxBalanceResponse
    @POST
    suspend fun placeOrder(
        @Header("X-BX-APIKEY") apiKey: String,
        @Url fullUrl: String
    ): BingxTradeResponse
}