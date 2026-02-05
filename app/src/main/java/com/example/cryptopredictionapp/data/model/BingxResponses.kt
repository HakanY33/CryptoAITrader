package com.example.cryptopredictionapp.data.model

import com.google.gson.annotations.SerializedName

// --- İŞTE EKSİK OLAN PARÇA BU ---
// İşlem açtığında veya genel bir hata aldığında bu döner
data class BingxResponse(
    val code: Int,
    val msg: String,
    val data: Any? = null
)

// Bakiye Cevabı
data class BingxBalanceResponse(
    val code: Int,
    val msg: String,
    val data: BalanceData?
)
data class BalanceData(
    val balance: BalanceDetail?
)
data class BalanceDetail(
    val equity: String?,     // Toplam Varlık (USDT)
    val availableMargin: String?, // Kullanılabilir Bakiye
    val balance: String?     // Bazen bu isimle gelebilir
)

// Fiyat (Ticker) Cevabı
data class BingxTickerResponse(
    val code: Int,
    val data: BingxTickerData?
)
data class BingxTickerData(
    val symbol: String,
    val lastPrice: String
)

// Mum (Kline) Cevabı
data class BingxKlineResponse(
    val code: Int,
    val data: List<BingxKlineData>?
)
data class BingxKlineData(
    val open: Double,
    val close: Double,
    val high: Double,
    val low: Double,
    val volume: Double,
    val time: Long
)

// Market Taraması
data class BingxMarketResponse(
    val code: Int,
    val data: List<BingxMarketItem>?
)
data class BingxMarketItem(
    val symbol: String,
    val lastPrice: String,
    val priceChangePercent: String?,
    val quoteVolume: String? // <-- BU SATIRI EKLE (Hacim verisi)
)


// Sembol Listesi
data class BingxContractResponse(
    val code: Int,
    val data: List<ContractData>?
)
data class ContractData(
    val symbol: String
)