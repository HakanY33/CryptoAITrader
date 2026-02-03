package com.example.cryptopredictionapp.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.example.cryptopredictionapp.util.Constants

object RetrofitClient {

    // AJAN BURADA: HttpLoggingInterceptor
    // Bu kod, tüm ağ trafiğini Logcat'e döker.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Tüm detayları (Header, Body, URL) göster
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // BingX Ana Adresi (Constants'tan alıyoruz)
    private val BASE_URL = Constants.BASE_URL

    val api: BingxApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // Ajanı (Client'ı) Retrofit'e ekledik
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BingxApi::class.java)
    }
}