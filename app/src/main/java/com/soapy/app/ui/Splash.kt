package com.soapy.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soapy.app.R
import kotlinx.coroutines.delay

/**
 * In-app splash. It draws on the very first frame, so the user never sees an empty screen
 * while the database and lock are loading. The root screen switches away after a short delay.
 */
@Composable
fun SplashContent() {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    val grow by animateFloatAsState(
        targetValue = if (started) 1f else 0.82f,
        animationSpec = tween(800),
        label = "splash-grow"
    )
    val fade by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(800),
        label = "splash-fade"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoapyColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = grow
                scaleY = grow
                alpha = fade
            }
        ) {
            Image(
                painter = painterResource(R.drawable.soapy_art),
                contentDescription = null,
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(32.dp))
            )
            Spacer(Modifier.height(24.dp))
            Text("Soapy", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text("Take your life back.", fontSize = 15.sp, color = SoapyColors.Lavender)
        }

        // Credit at the bottom: "By" stays put, the name types itself out underneath.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 44.dp)
        ) {
            Text("By", fontSize = 13.sp, color = SoapyColors.TextMuted)
            Spacer(Modifier.height(4.dp))
            TypingName(CREDIT_NAME)
        }
    }
}

private const val CREDIT_NAME = "Peter Wisdom K."

@Composable
private fun TypingName(name: String) {
    var typed by remember { mutableIntStateOf(0) }
    var cursorOn by remember { mutableStateOf(true) }

    LaunchedEffect(name) {
        delay(500)
        while (typed < name.length) {
            typed++
            delay(70)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(450)
            cursorOn = !cursorOn
        }
    }

    // The part not typed yet is drawn transparent, so the line keeps its final width
    // and stays centred instead of shifting as letters appear.
    val text = buildAnnotatedString {
        append(name.substring(0, typed))
        withStyle(SpanStyle(color = if (cursorOn) SoapyColors.Primary else Color.Transparent)) {
            append("|")
        }
        withStyle(SpanStyle(color = Color.Transparent)) {
            append(name.substring(typed))
        }
    }
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = SoapyColors.Lavender
    )
}
