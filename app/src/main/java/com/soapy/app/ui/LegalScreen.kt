package com.soapy.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Privacy Policy or Terms of Use, shown from the bundled text so it works offline. */
@Composable
fun LegalScreen(
    title: String,
    intro: String,
    sections: List<LegalSection>,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
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
                Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Last updated: ${LegalContent.UPDATED}",
                fontSize = 13.sp,
                color = SoapyColors.TextMuted,
                modifier = Modifier.padding(start = 12.dp)
            )
            Spacer(Modifier.height(16.dp))
            SoapyCard {
                Text(
                    intro,
                    fontSize = 14.sp,
                    color = SoapyColors.TextPrimary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        items(sections) { section ->
            Spacer(Modifier.height(24.dp))
            Text(
                section.heading,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = SoapyColors.Lavender
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                section.body.forEach { line ->
                    if (line.startsWith("• ")) {
                        Row {
                            Text("•", fontSize = 14.sp, color = SoapyColors.Primary)
                            Spacer(Modifier.width(10.dp))
                            Text(line.removePrefix("• "), fontSize = 14.sp, color = SoapyColors.TextPrimary)
                        }
                    } else {
                        Text(line, fontSize = 14.sp, color = SoapyColors.TextPrimary)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}
