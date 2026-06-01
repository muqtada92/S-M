package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File

@Composable
fun ChatBackground(
    backgroundType: String,
    presetId: String,
    customBgUri: String?,
    solidColorHex: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (backgroundType) {
            "PRESET" -> {
                when (presetId) {
                    "preset_cosmic" -> CosmicSpaceBackground()
                    "preset_sunset" -> SunsetGlowBackground()
                    "preset_forest" -> ForestSageBackground()
                    "preset_lavender" -> SweetLavenderBackground()
                    "preset_whatsapp_light" -> WhatsAppDoodleBackground(isDark = false)
                    "preset_whatsapp_dark" -> WhatsAppDoodleBackground(isDark = true)
                    else -> MinimalSlateBackground()
                }
            }
            "SOLID" -> {
                val color = remember(solidColorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(solidColorHex))
                    } catch (e: Exception) {
                        Color(0xFF0F172A)
                    }
                }
                Box(modifier = Modifier.fillMaxSize().background(color))
            }
            "GALLERY" -> {
                if (!customBgUri.isNullOrEmpty()) {
                    val file = remember(customBgUri) { File(customBgUri) }
                    if (file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = "Custom Chat Wallpaper",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        MinimalSlateBackground()
                    }
                } else {
                    MinimalSlateBackground()
                }
            }
            else -> MinimalSlateBackground()
        }
    }
}

@Composable
fun CosmicSpaceBackground() {
    // Elegant navy space backdrop with live pulsing stars
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val starAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star1"
    )
    val starAlpha2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star2"
    )

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF070B19),
            Color(0xFF0D152D),
            Color(0xFF161F3D)
        )
    )

    Canvas(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        // Render 22 beautiful stars manually
        val starCoords = listOf(
            Offset(0.1f, 0.15f), Offset(0.3f, 0.08f), Offset(0.55f, 0.2f),
            Offset(0.85f, 0.12f), Offset(0.25f, 0.32f), Offset(0.72f, 0.28f),
            Offset(0.08f, 0.48f), Offset(0.48f, 0.42f), Offset(0.9f, 0.52f),
            Offset(0.35f, 0.62f), Offset(0.68f, 0.58f), Offset(0.18f, 0.72f),
            Offset(0.82f, 0.76f), Offset(0.52f, 0.82f), Offset(0.28f, 0.9f),
            Offset(0.7f, 0.92f)
        )

        starCoords.forEachIndexed { index, offsetPercent ->
            val x = offsetPercent.x * size.width
            val y = offsetPercent.y * size.height
            val alpha = if (index % 2 == 0) starAlpha1 else starAlpha2
            val radius = if (index % 3 == 0) 3f else 2f

            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun SunsetGlowBackground() {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2D142C),
            Color(0xFF510A32),
            Color(0xFF801336),
            Color(0xFFC72C41),
            Color(0xFFEE4540)
        )
    )
    Box(modifier = Modifier.fillMaxSize().background(gradient))
}

@Composable
fun ForestSageBackground() {
    val gradient = Brush.verticalGradient(
         colors = listOf(
             Color(0xFF0F1E19),
             Color(0xFF1D352D),
             Color(0xFF2C4C41),
             Color(0xFF3F6659)
         )
    )
    Box(modifier = Modifier.fillMaxSize().background(gradient))
}

@Composable
fun SweetLavenderBackground() {
    val gradient = Brush.verticalGradient(
         colors = listOf(
             Color(0xFF1F1C2C),
             Color(0xFF2E2042),
             Color(0xFF4C3078),
             Color(0xFF6441A5)
         )
    )
    Box(modifier = Modifier.fillMaxSize().background(gradient))
}

@Composable
fun MinimalSlateBackground() {
    val gradient = Brush.verticalGradient(
         colors = listOf(
             Color(0xFFFEF7FF),
             Color(0xFFF3EDF7)
         )
    )
    Box(modifier = Modifier.fillMaxSize().background(gradient))
}

@Composable
fun WhatsAppDoodleBackground(isDark: Boolean) {
    val bgColor = if (isDark) Color(0xFF0B141A) else Color(0xFFEFEAE2)
    val strokeColor = if (isDark) Color(0xFF15222B) else Color(0xFFE4DEC3)

    Canvas(modifier = Modifier.fillMaxSize().background(bgColor)) {
        val width = size.width
        val height = size.height

        val cols = 5
        val rows = 8
        val cellW = width / cols
        val cellH = height / rows

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val seed = (r * cols + c) * 31
                val rX = ((seed % 17) / 17.0f - 0.5f) * (cellW * 0.4f)
                val rY = (((seed / 3) % 19) / 19.0f - 0.5f) * (cellH * 0.4f)
                val rScale = 0.5f + ((seed % 11) / 11.0f) * 0.4f
                val rRotation = ((seed % 360) - 180).toFloat()

                val centerX = c * cellW + cellW / 2 + rX
                val centerY = r * cellH + cellH / 2 + rY

                val type = seed % 8

                drawContext.canvas.save()
                drawContext.canvas.translate(centerX, centerY)
                drawContext.canvas.rotate(rRotation)

                val sizePx = 28f * rScale

                when (type) {
                    0 -> { // Draw chat bubble
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(-sizePx, -sizePx * 0.6f)
                            lineTo(sizePx, -sizePx * 0.6f)
                            lineTo(sizePx, sizePx * 0.6f)
                            lineTo(-sizePx * 0.2f, sizePx * 0.6f)
                            lineTo(-sizePx * 0.8f, sizePx)
                            lineTo(-sizePx * 0.8f, sizePx * 0.6f)
                            lineTo(-sizePx, sizePx * 0.6f)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                        )
                    }
                    1 -> { // Draw clock
                        drawCircle(
                            color = strokeColor,
                            radius = sizePx * 0.8f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                        )
                        drawLine(
                            color = strokeColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, -sizePx * 0.5f),
                            strokeWidth = 2.5f
                        )
                        drawLine(
                            color = strokeColor,
                            start = Offset(0f, 0f),
                            end = Offset(sizePx * 0.4f, 0f),
                            strokeWidth = 2.5f
                        )
                    }
                    2 -> { // Draw phone handset
                        drawArc(
                            color = strokeColor,
                            startAngle = -150f,
                            sweepAngle = 120f,
                            useCenter = false,
                            topLeft = Offset(-sizePx * 0.7f, -sizePx * 0.7f),
                            size = androidx.compose.ui.geometry.Size(sizePx * 1.4f, sizePx * 1.4f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                        )
                        drawLine(
                            color = strokeColor,
                            start = Offset(-sizePx * 0.5f, -sizePx * 0.2f),
                            end = Offset(-sizePx * 0.2f, -sizePx * 0.5f),
                            strokeWidth = 3f
                        )
                    }
                    3 -> { // Draw simple cloud
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(-sizePx * 0.5f, sizePx * 0.3f)
                            cubicTo(-sizePx * 0.9f, sizePx * 0.3f, -sizePx * 0.9f, -sizePx * 0.3f, -sizePx * 0.4f, -sizePx * 0.3f)
                            cubicTo(-sizePx * 0.4f, -sizePx * 0.7f, sizePx * 0.4f, -sizePx * 0.7f, sizePx * 0.4f, -sizePx * 0.2f)
                            cubicTo(sizePx * 0.8f, -sizePx * 0.2f, sizePx * 0.8f, sizePx * 0.3f, sizePx * 0.5f, sizePx * 0.3f)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                        )
                    }
                    4 -> { // Draw checkmark
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(-sizePx * 0.4f, 0f)
                            lineTo(-sizePx * 0.1f, sizePx * 0.3f)
                            lineTo(sizePx * 0.5f, -sizePx * 0.3f)
                        }
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.8f)
                        )
                    }
                    5 -> { // Draw star rays
                        for (i in 0 until 4) {
                            val angle = (i * 45).toDouble()
                            val sVal = Math.sin(Math.toRadians(angle)) * sizePx
                            val cVal = Math.cos(Math.toRadians(angle)) * sizePx
                            drawLine(
                                color = strokeColor,
                                start = Offset((-cVal * 0.4f).toFloat(), (-sVal * 0.4f).toFloat()),
                                end = Offset((cVal * 0.7f).toFloat(), (sVal * 0.7f).toFloat()),
                                strokeWidth = 2.5f
                            )
                        }
                    }
                    6 -> { // Draw mini heart
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, sizePx * 0.4f)
                            cubicTo(-sizePx * 0.8f, -sizePx * 0.5f, -sizePx * 0.2f, -sizePx, 0f, -sizePx * 0.2f)
                            cubicTo(sizePx * 0.2f, -sizePx, sizePx * 0.8f, -sizePx * 0.5f, 0f, sizePx * 0.4f)
                        }
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                        )
                    }
                    7 -> { // Draw camera
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(-sizePx * 0.7f, -sizePx * 0.3f)
                            lineTo(-sizePx * 0.3f, -sizePx * 0.3f)
                            lineTo(-sizePx * 0.2f, -sizePx * 0.5f)
                            lineTo(sizePx * 0.2f, -sizePx * 0.5f)
                            lineTo(sizePx * 0.3f, -sizePx * 0.3f)
                            lineTo(sizePx * 0.7f, -sizePx * 0.3f)
                            lineTo(sizePx * 0.7f, sizePx * 0.5f)
                            lineTo(-sizePx * 0.7f, sizePx * 0.5f)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                        )
                        drawCircle(
                            color = strokeColor,
                            radius = sizePx * 0.2f,
                            center = Offset(0f, sizePx * 0.1f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                        )
                    }
                }

                drawContext.canvas.restore()
            }
        }
    }
}
