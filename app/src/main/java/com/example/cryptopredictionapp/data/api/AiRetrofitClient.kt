package com.example.cryptopredictionapp.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

object AiRetrofitClient {

    //BİLGİSAYARIN IP ADRESİ
    private const val BASE_URL = "https://inconsequently-unexplainable-sharika.ngrok-free.dev"

    // Zaman aşımı ayarı (Yapay zeka bazen geç cevap verebilir, 30 saniye beklesin)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: AiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiApi::class.java)
    }
}