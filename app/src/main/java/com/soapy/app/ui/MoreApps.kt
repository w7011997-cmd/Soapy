package com.soapy.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soapy.app.R

private class PromoApp(val name: String, val packageName: String, val icon: Int) {
    // APKPure resolves a bare package name to the app's page. Not published there yet,
    // so until it is, the link may show "not found".
    val url: String get() = "https://apkpure.com/p/$packageName"
}

private val Discovr = PromoApp("Discovr", "com.discovr.app", R.drawable.app_discovr)
private val Chatify = PromoApp("Chatify", "com.chatify.app", R.drawable.app_chatify)
private val Resellr = PromoApp("Resellr", "com.reselit.app", R.drawable.app_resellr)

private fun openStorePage(context: Context, app: PromoApp) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(app.url)))
    } catch (_: Exception) {
        // no browser or store installed: nothing to do
    }
}

/** A personal note from the developer, with links to his other apps. */
@Composable
fun MoreAppsSection() {
    val context = LocalContext.current
    val open: (PromoApp) -> Unit = { openStorePage(context, it) }

    SoapyCard {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "A note from Wisdom",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = SoapyColors.Lavender
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Hey, my name is Wisdom. I sincerely want to help you, and I want you to also " +
                    "help me help you. I can't do this job alone. Fight the urge.",
                fontSize = 14.sp,
                color = SoapyColors.TextPrimary
            )

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(Discovr, Chatify, Resellr).forEach { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { open(app) }
                            .padding(6.dp)
                    ) {
                        Image(
                            painter = painterResource(app.icon),
                            contentDescription = app.name,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(15.dp))
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(app.name, fontSize = 12.sp, color = SoapyColors.TextMuted)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            PromoBlock(
                message = "Are you a student? If yes, download my app that's built " +
                    "particularly for students.",
                app = Discovr,
                onOpen = open
            )
            Spacer(Modifier.height(20.dp))
            PromoBlock(
                message = "If you like anonymous chatting, download my app, Chatify.",
                app = Chatify,
                onOpen = open
            )
            Spacer(Modifier.height(20.dp))
            PromoBlock(
                message = "Or do you do drop shipping? If yes, download my app, Resellr, and " +
                    "start making money now by selling legal products that are or are not yours.",
                app = Resellr,
                onOpen = open
            )
        }
    }
}

@Composable
private fun PromoBlock(message: String, app: PromoApp, onOpen: (PromoApp) -> Unit) {
    Text(message, fontSize = 14.sp, color = SoapyColors.TextPrimary)
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SoapyColors.SurfaceHigh)
            .clickable { onOpen(app) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(app.icon),
            contentDescription = app.name,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(app.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("Get it on APKPure", fontSize = 12.sp, color = SoapyColors.TextMuted)
        }
        Text(
            "Download",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = SoapyColors.Primary
        )
    }
}
