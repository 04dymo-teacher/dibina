package com.example.dibina.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * High-fidelity representation of IMG 2:
 * DIBINA Logotype ("D" book mark + "IBINA" + "Diary Kebiasaan Anak").
 *
 * CRITICAL LOGO RULE MANDATE:
 * The DIBINA logo is a LOGOTYPE.
 * If the logo already contains the word "DIBINA", DO NOT write "DIBINA" again beside or below the logo.
 */
@Composable
fun DibinaLogotypeView(
    modifier: Modifier = Modifier,
    scale: Float = 1.0f,
    showSubtitle: Boolean = true
) {
    val brandBlue = Color(0xFF0288D1)
    val darkBrandBlue = Color(0xFF01579B)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            // Book "D" Mark (IMG 1 / first letter of IMG 2)
            Canvas(
                modifier = Modifier
                    .size((48 * scale).dp, (56 * scale).dp)
                    .padding(end = (4 * scale).dp)
            ) {
                val w = size.width
                val h = size.height

                // Fanning pages at top
                val page1 = Path().apply {
                    moveTo(w * 0.35f, h * 0.28f)
                    lineTo(w * 0.85f, h * 0.04f)
                    lineTo(w * 0.90f, h * 0.12f)
                    lineTo(w * 0.40f, h * 0.32f)
                    close()
                }
                drawPath(page1, brandBlue)

                val page2 = Path().apply {
                    moveTo(w * 0.28f, h * 0.35f)
                    lineTo(w * 0.95f, h * 0.18f)
                    lineTo(w * 0.97f, h * 0.26f)
                    lineTo(w * 0.35f, h * 0.40f)
                    close()
                }
                drawPath(page2, brandBlue)

                val page3 = Path().apply {
                    moveTo(w * 0.25f, h * 0.42f)
                    lineTo(w * 1.00f, h * 0.34f)
                    lineTo(w * 1.00f, h * 0.43f)
                    lineTo(w * 0.30f, h * 0.45f)
                    close()
                }
                drawPath(page3, brandBlue)

                // Main "D" Body
                val dPath = Path().apply {
                    moveTo(w * 0.15f, h * 0.36f)
                    // Spine
                    lineTo(w * 0.25f, h * 0.95f)
                    lineTo(w * 0.60f, h * 0.95f)
                    cubicTo(w * 0.95f, h * 0.95f, w * 1.00f, h * 0.75f, w * 1.00f, h * 0.62f)
                    cubicTo(w * 1.00f, h * 0.45f, w * 0.85f, h * 0.42f, w * 0.50f, h * 0.42f)
                    lineTo(w * 0.25f, h * 0.42f)
                    close()
                }
                drawPath(dPath, brandBlue)

                // Inner Counter of "D"
                val innerCounter = Path().apply {
                    moveTo(w * 0.42f, h * 0.58f)
                    lineTo(w * 0.60f, h * 0.58f)
                    cubicTo(w * 0.75f, h * 0.58f, w * 0.80f, h * 0.65f, w * 0.80f, h * 0.70f)
                    cubicTo(w * 0.80f, h * 0.78f, w * 0.72f, h * 0.82f, w * 0.60f, h * 0.82f)
                    lineTo(w * 0.42f, h * 0.82f)
                    close()
                }
                drawPath(innerCounter, Color.White)
            }

            // Word "IBINA" in bold custom blue typeface
            Text(
                text = "IBINA",
                fontSize = (44 * scale).sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = brandBlue,
                letterSpacing = (-0.5).sp,
                lineHeight = (44 * scale).sp
            )
        }

        if (showSubtitle) {
            Spacer(modifier = Modifier.height((4 * scale).dp))
            Text(
                text = "D i a r y   K e b i a s a a n   A n a k",
                fontSize = (11 * scale).sp,
                fontWeight = FontWeight.Bold,
                color = darkBrandBlue,
                letterSpacing = 1.5.sp
            )
        }
    }
}
