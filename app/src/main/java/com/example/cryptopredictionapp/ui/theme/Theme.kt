package com.example.cryptopredictionapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Sadece Dark Mode kullanacağız (Cyberpunk aydınlık olmaz!)
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryNeon,
    secondary = SecondaryNeon,
    background = Color.Transparent, // Arka planı biz çizeceğiz
    surface = Color.Transparent,    // Kartları biz cam yapacağız
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun CryptoPredictionAppTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Type.kt dosyan varsa oradan alır, yoksa default
        content = {
            // --- GLOBAL LIQUID BACKGROUND ---
            // Tüm uygulamanın arkasına o "Mesh Gradient"i burada atıyoruz.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                DeepViolet, // Üst kısım
                                RoyalBlue,  // Orta kısım
                                VoidBlack   // Alt kısım
                            )
                        )
                    )
            ) {
                // İstersen buraya "RadialGradient" ile yüzen baloncuklar da ekleyebiliriz
                // ama şimdilik performans için Linear Gradient yeterli.
                content()
            }
        }
    )
}