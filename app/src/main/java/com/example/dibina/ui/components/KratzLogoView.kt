package com.example.dibina.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Representation of IMG 3:
 * Developer branding: "Kratz" with the stylized flame swirl and "-Karangtaluntroopz-".
 * Used for Developer Splash and Settings > Tentang.
 */
@Composable
fun KratzLogoView(
    modifier: Modifier = Modifier,
    scale: Float = 1.0f
) {
    val charcoal = Color(0xFF222222)
    val cyanFlame = Color(0xFF00E5FF)
    val blueFlame = Color(0xFF0D47A1)
    val vibrantBlue = Color(0xFF0288D1)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Krat",
                fontSize = (38 * scale).sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = charcoal,
                letterSpacing = (-1).sp
            )

            // Stylized 'o' with glowing blue/cyan flame
            Canvas(
                modifier = Modifier
                    .size((36 * scale).dp, (44 * scale).dp)
                    .padding(horizontal = (1 * scale).dp)
            ) {
                val w = size.width
                val h = size.height

                // Flame rising from 'o'
                val flamePath = Path().apply {
                    moveTo(w * 0.5f, h * 0.1f)
                    cubicTo(w * 0.7f, h * 0.0f, w * 0.9f, h * 0.2f, w * 0.8f, h * 0.45f)
                    cubicTo(w * 1.0f, h * 0.55f, w * 0.9f, h * 0.85f, w * 0.6f, h * 0.95f)
                    cubicTo(w * 0.3f, h * 0.95f, w * 0.1f, h * 0.7f, w * 0.15f, h * 0.5f)
                    cubicTo(w * 0.1f, h * 0.35f, w * 0.3f, h * 0.2f, w * 0.5f, h * 0.1f)
                    close()
                }

                val flameBrush = Brush.verticalGradient(
                    colors = listOf(cyanFlame, vibrantBlue, blueFlame),
                    startY = 0f,
                    endY = h
                )
                drawPath(flamePath, flameBrush)

                // Inner circle eye of 'o'
                drawCircle(
                    color = Color.White,
                    radius = w * 0.22f,
                    center = Offset(w * 0.52f, h * 0.65f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(vibrantBlue, blueFlame),
                        center = Offset(w * 0.52f, h * 0.65f),
                        radius = w * 0.15f
                    ),
                    radius = w * 0.15f,
                    center = Offset(w * 0.52f, h * 0.65f)
                )
            }

            Text(
                text = "z",
                fontSize = (38 * scale).sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = charcoal,
                letterSpacing = (-1).sp
            )
        }

        Spacer(modifier = Modifier.height((2 * scale).dp))

        Text(
            text = "-Karangtaluntroopz-",
            fontSize = (13 * scale).sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.SansSerif,
            color = charcoal.copy(alpha = 0.85f),
            letterSpacing = 1.sp
        )
    }
}
