package com.example.cryptopredictionapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modern, Temiz Cam Efekti.
 * Bulanıklık (Blur) YOK. Sadece şeffaf mat doku.
 * Böylece yazılar net okunacak.
 */
fun Modifier.glassEffect(
    cornerRadius: Dp = 20.dp,
    opacity: Float = 0.08f // %8 Beyazlık (Hafif bir cam filmi gibi)
) = composed {
    val shape = RoundedCornerShape(cornerRadius)

    this
        // Blur (RenderEffect) kısmını kaldırdık! Artık içeriği bozmayacak.
        .clip(shape)
        .background(
            // Düz renk. Arka planı hafifçe aydınlatır.
            color = Color.White.copy(alpha = opacity)
        )
        .border(
            width = 1.dp,
            // O "Premium" hissi veren ince kenarlık burada
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.4f), // Üstü parlak
                    Color.White.copy(alpha = 0.05f) // Altı sönük
                )
            ),
            shape = shape
        )
}