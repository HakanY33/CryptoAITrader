package com.example.cryptopredictionapp.data.api

import com.example.cryptopredictionapp.data.model.*
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface BingxApi {
    // 1. Fiyat Çekme
    @GET("openApi/swap/v2/quote/ticker")
    suspend fun getTicker(@Query("symbol") symbol: String): BingxTickerResponse

    // 2. Mum (Grafik) Çekme
    @GET("openApi/swap/v3/quote/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 1000
    ): BingxKlineResponse

    // 3. Sembol Listesi
    @GET("openApi/swap/v2/quote/contracts")
    suspend fun getContracts(): BingxContractResponse

    // 4. Piyasa Taraması
    @GET("openApi/swap/v2/quote/ticker")
    suspend fun getMarketScanData(): BingxMarketResponse

    // 5. Bakiye (Dinamik URL ile)
    @GET
    suspend fun getBalance(
        @Header("X-BX-APIKEY") apiKey: String,
        @Url fullUrl: String
    ): BingxBalanceResponse

    // 6. İŞLEM AÇMA (SENİN İSTEDİĞİN MANUEL YAPI)
    // Repository'den gelen hazır ve imzalı linki (fullUrl) direkt çalıştırır.
    @POST
    suspend fun placeOrder(
        @Header("X-BX-APIKEY") apiKey: String,
        @Url fullUrl: String
    ): BingxResponse
}