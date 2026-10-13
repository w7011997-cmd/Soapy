package com.soapy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    lockEnabled: Boolean,
    adminOn: Boolean,
    protectionOn: Boolean,
    onBack: () -> Unit,
    onSetupLock: () -> Unit,
    onChangePin: () -> Unit,
    onDisableLock: () -> Unit,
    onEnableAdmin: () -> Unit,
    onEnableProtection: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit
) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty()
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
                Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            SettingsSection("App lock") {
                Muted(
                    if (lockEnabled) {
                        "Soapy asks for your PIN each time it opens."
                    } else {
                        "Ask for a PIN each time Soapy opens. You will also set a security " +
                            "question, so you can reset the PIN if you forget it."
                    }
                )
                Spacer(Modifier.height(14.dp))
                if (!lockEnabled) {
                    PrimaryButton(text = "Set up lock", onClick = onSetupLock)
                } else {
                    StatusLine(text = "Lock is on", on = true)
                    Spacer(Modifier.height(14.dp))
                    SecondaryButton(text = "Change PIN", onClick = onChangePin)
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(text = "Turn off lock", onClick = onDisableLock)
                }
            }
        }

        item {
            SettingsSection("Uninstall protection") {
                Muted(
                    "Makes Soapy harder to uninstall. Android will make you deactivate it first, " +
                        "and Soapy will ask why you are leaving."
                )
                Spacer(Modifier.height(14.dp))
                if (adminOn) {
                    StatusLine(text = "On", on = true)
                } else {
                    PrimaryButton(text = "Turn on", onClick = onEnableAdmin)
                }
            }
        }

        item {
            SettingsSection("Blocking") {
                Muted("Soapy needs its accessibility service to catch blocked words in your browser.")
                Spacer(Modifier.height(14.dp))
                if (protectionOn) {
                    StatusLine(text = "On", on = true)
                } else {
                    PrimaryButton(text = "Turn on", onClick = onEnableProtection)
                }
            }
        }

        item {
            SettingsSection("About") {
                Muted("Soapy $version")
                Spacer(Modifier.height(4.dp))
                Muted("Your words and sites stay on this phone. Once added, they can't be removed.")
            }
        }

        item {
            SettingsSection("Legal") {
                LegalRow(text = "Privacy Policy", onClick = onOpenPrivacy)
                HorizontalDivider(color = SoapyColors.Outline)
                LegalRow(text = "Terms of Use", onClick = onOpenTerms)
            }
        }

        item { MoreAppsSection() }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    SoapyCard {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = SoapyColors.Lavender
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun LegalRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text("›", fontSize = 22.sp, color = SoapyColors.Lavender)
    }
}

@Composable
private fun Muted(text: String) {
    Text(text, fontSize = 14.sp, color = SoapyColors.TextMuted)
}

@Composable
private fun StatusLine(text: String, on: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (on) SoapyColors.Success else SoapyColors.Warning)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (on) SoapyColors.Success else SoapyColors.Warning
        )
    }
}
