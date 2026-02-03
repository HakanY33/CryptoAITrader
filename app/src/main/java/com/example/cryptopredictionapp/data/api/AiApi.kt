package com.example.cryptopredictionapp.data.api

import com.example.cryptopredictionapp.data.model.AiResponse
import com.example.cryptopredictionapp.data.model.MarketDataRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AiApi {

    // Python sunucusundaki @app.post("/ask-ai") adresine istek atar
    @POST("ask-ai")
    suspend fun askGemini(
        @Body request: MarketDataRequest
    ): AiResponse
}