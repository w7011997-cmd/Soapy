package com.soapy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soapy.app.data.DAY_MS
import com.soapy.app.data.ProgressMath
import com.soapy.app.data.ProgressStats
import kotlinx.coroutines.delay

/** Current time, refreshed every 30 seconds so the streak keeps counting while the app is open. */
@Composable
fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    return now
}

private fun dayWord(n: Int) = if (n == 1) "day" else "days"

/** Compact streak card for the home screen. Tap to open the Progress screen. */
@Composable
fun StreakCard(stats: ProgressStats, blocksWeek: Int, onClick: () -> Unit) {
    SoapyCard(
        container = SoapyColors.Card,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Your streak", fontSize = 13.sp, color = SoapyColors.Lavender)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${stats.streakDays}", fontSize = 44.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        dayWord(stats.streakDays),
                        fontSize = 16.sp,
                        color = SoapyColors.TextMuted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (stats.streakDays in ProgressMath.MILESTONES) {
                    Text(
                        "Milestone reached. Keep going.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoapyColors.Success
                    )
                } else {
                    Text(
                        "$blocksWeek ${if (blocksWeek == 1) "urge" else "urges"} stopped this week",
                        fontSize = 13.sp,
                        color = SoapyColors.TextMuted
                    )
                }
            }
            Text("›", fontSize = 28.sp, color = SoapyColors.Lavender)
        }
    }
}

@Composable
fun ProgressScreen(
    stats: ProgressStats,
    blocksTotal: Int,
    blocksToday: Int,
    blocksWeek: Int,
    onBack: () -> Unit,
    onSlip: () -> Unit
) {
    var confirmSlip by remember { mutableStateOf(false) }

    if (confirmSlip) {
        AlertDialog(
            onDismissRequest = { confirmSlip = false },
            title = { Text("Reset your streak?") },
            text = {
                Text(
                    "A slip is not the end. Your longest streak and your history stay. " +
                        "Being honest with yourself is what makes this work. Get back up."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmSlip = false
                    onSlip()
                }) { Text("Yes, reset") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSlip = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SoapyColors.Lavender
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text("Progress", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        item { StreakDetail(stats) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("${stats.longestDays}", "Longest streak (days)", Modifier.weight(1f))
                StatTile("${stats.sinceStartDays}", "Days since you started", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("$blocksTotal", "Urges stopped, total", Modifier.weight(1f))
                StatTile("$blocksWeek", "Urges stopped, last 7 days", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("$blocksToday", "Urges stopped today", Modifier.weight(1f))
                StatTile("${stats.slipCount}", "Slips", Modifier.weight(1f))
            }
        }

        item {
            SoapyCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Milestones",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoapyColors.Lavender
                    )
                    Spacer(Modifier.height(12.dp))
                    ProgressMath.MILESTONES.chunked(5).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { m ->
                                MilestoneChip(
                                    label = if (m == 365) "1 yr" else "${m}d",
                                    reached = stats.longestDays >= m,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        "Highlighted ones are milestones you have already reached.",
                        fontSize = 12.sp,
                        color = SoapyColors.TextMuted
                    )
                }
            }
        }

        item {
            SoapyCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Had a slip?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoapyColors.Lavender
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Soapy can only see your browsers, so your streak depends on your honesty. " +
                            "If you slipped, reset it. Starting again is part of quitting.",
                        fontSize = 14.sp,
                        color = SoapyColors.TextMuted
                    )
                    Spacer(Modifier.height(14.dp))
                    SecondaryButton(text = "I slipped", onClick = { confirmSlip = true })
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun StreakDetail(stats: ProgressStats) {
    SoapyCard(container = SoapyColors.Card) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Current streak", fontSize = 13.sp, color = SoapyColors.Lavender)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${stats.streakDays}", fontSize = 56.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Text(
                    dayWord(stats.streakDays),
                    fontSize = 18.sp,
                    color = SoapyColors.TextMuted,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            Text(
                "and ${stats.streakHours} ${if (stats.streakHours == 1) "hour" else "hours"}",
                fontSize = 14.sp,
                color = SoapyColors.TextMuted
            )

            val next = ProgressMath.nextMilestone(stats.streakDays)
            Spacer(Modifier.height(16.dp))
            if (next == null) {
                Text(
                    "You have passed every milestone. Keep going.",
                    fontSize = 14.sp,
                    color = SoapyColors.Success
                )
            } else {
                val prev = ProgressMath.previousMilestone(stats.streakDays)
                val daysFloat = stats.streakMs.toFloat() / DAY_MS
                val fraction = ((daysFloat - prev) / (next - prev)).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = SoapyColors.Primary,
                    trackColor = SoapyColors.Outline
                )
                Spacer(Modifier.height(8.dp))
                val left = next - stats.streakDays
                Text(
                    "Next milestone: $next ${dayWord(next)} ($left ${dayWord(left)} to go)",
                    fontSize = 13.sp,
                    color = SoapyColors.TextMuted
                )
            }
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    SoapyCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 12.sp, color = SoapyColors.TextMuted)
        }
    }
}

@Composable
private fun MilestoneChip(label: String, reached: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (reached) SoapyColors.Lavender else SoapyColors.TextMuted,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (reached) SoapyColors.PrimaryContainer else SoapyColors.SurfaceHigh)
            .padding(vertical = 10.dp),
        textAlign = TextAlign.Center
    )
}

/** Shown once, before the first home screen, so people who already have clean days can say so. */
@Composable
fun ProgressSetupScreen(onStart: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val days = text.toIntOrNull()
    val valid = text.isEmpty() || (days != null && days in 0..365)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Start your streak", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            "Soapy counts the days you stay clean. If you have already been clean for a while, " +
                "enter how many days. Leave it empty to start from today.",
            fontSize = 15.sp,
            color = SoapyColors.TextMuted
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { v -> if (v.length <= 3 && v.all { it.isDigit() }) text = v },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Days already clean") },
            singleLine = true,
            isError = !valid,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = { Text("0 to 365. Be honest, this number is only for you.") }
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton(text = "Start", onClick = { if (valid) onStart(days ?: 0) })
    }
}
