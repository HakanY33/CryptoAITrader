package com.example.cryptopredictionapp.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // BingX Ana Adresi
    private const val BASE_URL = "https://open-api.bingx.com/"

    // Retrofit nesnesini oluşturuyoruz
    val api: BingxApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // JSON çevirici
            .build()
            .create(BingxApi::class.java)
    }
}