package com.docscan.pro.feature.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Indigo = Color(0xFF3E4CC0)
private val Teal = Color(0xFF1FC6A0)
private val LineIdle = Color(0xFF4A58C4)
private val Fold = Color(0xFFC9CEF5)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onFinished()
    }

    val transition = rememberInfiniteTransition(label = "scan")
    val beam by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "beam",
    )

    Box(Modifier.fillMaxSize().drawBehind { drawRect(Indigo) }) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(Modifier.size(150.dp)) {
                val u = size.minDimension / 108f
                fun p(v: Float) = v * u

                // Document + folded corner
                drawPath(
                    Path().apply {
                        moveTo(p(38f), p(26f)); lineTo(p(60f), p(26f)); lineTo(p(72f), p(38f))
                        lineTo(p(72f), p(82f)); lineTo(p(38f), p(82f)); close()
                    },
                    Color.White,
                )
                drawPath(
                    Path().apply {
                        moveTo(p(60f), p(26f)); lineTo(p(72f), p(38f)); lineTo(p(60f), p(38f)); close()
                    },
                    Fold,
                )

                // Beam position sweeps from the top to the bottom of the document.
                val beamY = 26f + beam * (82f - 26f)

                // Text lines: turn teal ("scanned") once the beam has passed them — ambient effect.
                listOf(Triple(44f, 45f, 22f), Triple(44f, 53f, 22f), Triple(44f, 61f, 15f)).forEach { (lx, ly, lw) ->
                    val color = if (ly <= beamY) Teal else LineIdle
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(p(lx), p(ly)),
                        size = Size(p(lw), p(3.5f)),
                        cornerRadius = CornerRadius(p(1.75f)),
                    )
                }

                // The scan beam
                drawRoundRect(
                    color = Teal,
                    topLeft = Offset(p(30f), p(beamY) - p(2f)),
                    size = Size(p(48f), p(4f)),
                    cornerRadius = CornerRadius(p(2f)),
                )
            }

            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("DocScan ", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                Text("Pro", color = Teal, fontSize = 26.sp, fontWeight = FontWeight.Medium)
            }
        }

        Text(
            "Free document scanner",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
        )
    }
}
