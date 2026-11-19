package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.example.ui.theme.GopherFontFamily
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Crisp BeBoss Brand Reveal Splash Screen featuring a fast sweeping arc
 * around the official BeBoss geometric emblem.
 */
@Composable
fun AppSplashScreen(
    isDarkTheme: Boolean = false,
    durationMs: Long = 520L,
    onSplashFinished: () -> Unit = {}
) {
    val logoScale = remember { Animatable(0.86f) }
    val contentAlpha = remember { Animatable(0f) }
    val arcSweep = remember { Animatable(0f) }

    val bgTop = if (isDarkTheme) Color(0xFF0A0F1D) else Color(0xFFFFFBF7)
    val bgBottom = if (isDarkTheme) Color(0xFF11192B) else Color(0xFFF6F2EC)
    val onBg = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val muted = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    LaunchedEffect(Unit) {
        launch {
            logoScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
        }
        launch {
            contentAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
        }
        launch {
            arcSweep.animateTo(
                targetValue = 300f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
        }
        delay(durationMs)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(contentAlpha.value)
                .padding(bottom = 36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(134.dp)
                    .scale(logoScale.value),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(130.dp)) {
                    val strokePx = 4.dp.toPx()
                    drawCircle(
                        color = OrangePrimary.copy(alpha = 0.14f),
                        style = Stroke(width = strokePx)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(OrangePrimary, ProfitGreen, OrangePrimary),
                            center = Offset(size.width / 2f, size.height / 2f)
                        ),
                        startAngle = -90f,
                        sweepAngle = arcSweep.value,
                        useCenter = false,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.5.dp, OrangePrimary.copy(alpha = 0.55f)),
                    shadowElevation = 10.dp,
                    modifier = Modifier.size(92.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.beboss_app_logo_1787833759468),
                        contentDescription = "BeBoss POS Emblem",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "BeBoss",
                fontFamily = GopherFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = onBg,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Multi-Branch POS & Inventory Suite",
                fontFamily = GopherFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.5.sp,
                color = muted,
                letterSpacing = 0.4.sp
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .alpha(contentAlpha.value),
            shape = RoundedCornerShape(999.dp),
            color = OrangePrimary.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.28f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(ProfitGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BEBOSS ENTERPRISE POS",
                    fontFamily = GopherFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = OrangePrimary,
                    letterSpacing = 1.4.sp
                )
            }
        }
    }
}
