package com.example.cryptopredictionapp.ui.components

import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.cryptopredictionapp.data.model.BingxKlineData
import com.example.cryptopredictionapp.ui.viewmodel.AnalysisState
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import java.math.BigDecimal

@Composable
fun CryptoChart(
    candles: List<BingxKlineData>,
    analysisState: AnalysisState
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp), // Grafiğin yüksekliği
        factory = { context ->
            CandleStickChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                axisLeft.setDrawGridLines(false)
                axisLeft.textColor = Color.WHITE
                axisRight.isEnabled = false // Sağdaki ekseni kapat
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.textColor = Color.WHITE
                setPinchZoom(true)
                isDragEnabled = true
                setScaleEnabled(true)
                // 1. ARKA PLANI ŞEFFAF YAP
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setGridBackgroundColor(android.graphics.Color.TRANSPARENT)
                setDrawGridBackground(false)

                // 2. KENARLIKLARI KALDIR (Sadece mumlar kalsın)
                axisLeft.setDrawGridLines(false)
                axisLeft.textColor = android.graphics.Color.WHITE // Yazılar beyaz olsun
                xAxis.setDrawGridLines(false)
                xAxis.textColor = android.graphics.Color.WHITE


            }
        },
        update = { chart ->
            if (candles.isNotEmpty()) {
                // Mum Verilerini Hazırla (MPAndroidChart formatına çevir)
                // Gelen veri eskiden yeniye olmalı
                val entries = ArrayList<CandleEntry>()
                // API genelde tersten verir, o yüzden reversed() kontrolü yapıyoruz
                // Bizim repository reversed veriyor olabilir, kontrol et.
                // Eğer grafik ters çıkarsa buradaki reversed()'ı kaldır.
                val sortedCandles = candles.sortedBy { it.time }

                sortedCandles.forEachIndexed { index, kline ->
                    entries.add(
                        CandleEntry(
                            index.toFloat(),
                            kline.high.toFloat(),
                            kline.low.toFloat(),
                            kline.open.toFloat(),
                            kline.close.toFloat()
                        )
                    )
                }

                val dataSet = CandleDataSet(entries, "Fiyat").apply {
                    color = Color.rgb(80, 80, 80)
                    shadowColor = Color.DKGRAY
                    shadowWidth = 0.7f
                    decreasingColor = Color.RED
                    decreasingPaintStyle = Paint.Style.FILL
                    increasingColor = Color.GREEN
                    increasingPaintStyle = Paint.Style.FILL
                    neutralColor = Color.BLUE
                    setDrawValues(false) // Mumların üstüne rakam yazma, karışır
                }

                // --- TP / SL / ENTRY ÇİZGİLERİ (Limit Lines) ---
                val leftAxis = chart.axisLeft
                leftAxis.removeAllLimitLines() // Eskileri temizle

                // Eğer analizde TP/SL varsa çiz
                if (analysisState.tradeTp.isNotEmpty()) {
                    try {
                        val tpPrice = analysisState.tradeTp.toFloat()
                        val slPrice = analysisState.tradeSl.toFloat()
                        val entryPrice = analysisState.tradeEntry.toFloat()

                        val tpLine = LimitLine(tpPrice, "TP").apply {
                            lineWidth = 2f
                            lineColor = Color.GREEN
                            textColor = Color.GREEN
                            textSize = 12f
                        }

                        val slLine = LimitLine(slPrice, "SL").apply {
                            lineWidth = 2f
                            lineColor = Color.RED
                            textColor = Color.RED
                            textSize = 12f
                        }

                        val entryLine = LimitLine(entryPrice, "Giriş").apply {
                            lineWidth = 1f
                            lineColor = Color.YELLOW
                            textColor = Color.YELLOW
                            enableDashedLine(10f, 10f, 0f) // Kesikli çizgi
                        }

                        leftAxis.addLimitLine(tpLine)
                        leftAxis.addLimitLine(slLine)
                        leftAxis.addLimitLine(entryLine)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                chart.data = CandleData(dataSet)
                chart.invalidate() // Grafiği yenile

                // Grafiği en sona (son muma) odakla
                chart.moveViewToX(entries.size.toFloat())
                chart.setVisibleXRangeMaximum(50f) // Ekranda en fazla 50 mum göster (Zoomlu başla)
            }
        }
    )
}