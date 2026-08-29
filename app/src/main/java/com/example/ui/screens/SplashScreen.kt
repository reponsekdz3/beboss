package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.GopherFontFamily
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.OrangePrimary
import kotlinx.coroutines.delay

/**
 * Instagram-Style Launch & Splash Screen with a continuous animated gradient circle
 * rotating and pulsing around the BeBoss app icon.
 */
@Composable
fun AppSplashScreen(
    isDarkTheme: Boolean = false,
    durationMs: Long = 1800L,
    onSplashFinished: () -> Unit = {}
) {
    val logoScale = remember { Animatable(0.75f) }
    val textAlpha = remember { Animatable(0f) }

    // Instagram signature sunset gradient ring palette
    val instagramGradientColors = listOf(
        Color(0xFFF58529), // Warm Orange
        Color(0xFFFEDA77), // Golden Amber
        Color(0xFFDD2A7B), // Magenta Pink
        Color(0xFF8134AF), // Vibrant Purple
        Color(0xFF515BD4), // Royal Blue
        Color(0xFFF58529)  // Loop Back to Orange
    )

    // Infinite rotating animation for the circle around the icon
    val infiniteTransition = rememberInfiniteTransition(label = "InstagramRingTransition")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RingPulse"
    )

    val strokeWidthPulse by infiniteTransition.animateFloat(
        initialValue = 3.2f,
        targetValue = 4.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StrokePulse"
    )

    LaunchedEffect(Unit) {
        // Pop in the logo with spring bounce
        logoScale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = 0.65f,
                stiffness = 300f
            )
        )
        textAlpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(400)
        )
        // Wait for splash duration then transition to destination
        delay(durationMs)
        onSplashFinished()
    }

    val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFFFFFFF)
    val textColor = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val subtitleColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Center Content: Animated Instagram Ring Loader + App Icon + Brand Name
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 40.dp)
        ) {
            // Icon + Circular Animated Ring Container
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(logoScale.value),
                contentAlignment = Alignment.Center
            ) {
                // Animated Circular Loading Ring around Icon (Instagram Style)
                Canvas(
                    modifier = Modifier
                        .size(136.dp)
                        .scale(pulseScale)
                        .rotate(rotationAngle)
                ) {
                    val sweepBrush = Brush.sweepGradient(
                        colors = instagramGradientColors,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )

                    // Draw outer vibrant spinning arc with rounded caps
                    drawArc(
                        brush = sweepBrush,
                        startAngle = 0f,
                        sweepAngle = 300f,
                        useCenter = false,
                        style = Stroke(
                            width = strokeWidthPulse.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    // Draw secondary subtle trailing shimmer arc
                    drawArc(
                        brush = sweepBrush,
                        startAngle = 320f,
                        sweepAngle = 20f,
                        useCenter = false,
                        style = Stroke(
                            width = (strokeWidthPulse * 0.8f).dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }

                // Inner App Icon Surface with smooth rounded corners and elevation
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFFFFFFF),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(26.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFF7ED),
                                        Color(0xFFFFFFFF)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.beboss_app_logo_1787833759468),
                            contentDescription = "BeBoss App Icon",
                            modifier = Modifier
                                .size(82.dp)
                                .clip(RoundedCornerShape(22.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App Brand Name & Subtitle with smooth fade-in
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(textAlpha.value)
            ) {
                Text(
                    text = "BeBoss",
                    fontFamily = GopherFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = textColor,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Smart POS & Business Hub",
                    fontFamily = GopherFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.5.sp,
                    color = subtitleColor,
                    letterSpacing = 0.8.sp
                )
            }
        }

        // Bottom Branding Section: "from BEBOSS" (Instagram style)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "from",
                fontFamily = GopherFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.5.sp,
                color = subtitleColor.copy(alpha = 0.75f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BEBOSS",
                    fontFamily = GopherFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = OrangePrimary,
                    letterSpacing = 2.5.sp
                )
            }
        }
    }
}
