package com.example.cryptopredictionapp.util

object Constants {
    // --- AYARLAR ---
    // BURASI "true" İSE DEMO (VST), "false" İSE GERÇEK PARA (USDT) KULLANIR
    var IS_DEMO_MODE = true

    // API Anahtarların (Genelde hem VST hem Real için aynıdır ama güvenlik için kontrol et)
    const val API_KEY = "BURAYA_API_KEY_GELECEK"
    const val SECRET_KEY = "BURAYA_SECRET_KEY_GELECEK"

    // --- ADRESLER ---
    private const val VST_URL = "https://open-api-vst.bingx.com/" // Sanal Para
    private const val REAL_URL = "https://open-api.bingx.com/"    // Gerçek Para

    // --- DİNAMİK URL SEÇİCİ ---
    // Retrofit client'ı oluştururken bunu çağıracağız
    val BASE_URL: String
        get() = if (IS_DEMO_MODE) VST_URL else REAL_URL
}