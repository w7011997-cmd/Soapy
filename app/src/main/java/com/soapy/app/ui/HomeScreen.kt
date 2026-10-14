package com.soapy.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soapy.app.R
import com.soapy.app.data.BlockedKeyword
import com.soapy.app.data.ParsedTerm
import com.soapy.app.data.ProgressStats
import com.soapy.app.data.TermParser

@Composable
fun HomeScreen(
    keywords: List<BlockedKeyword>,
    protectionOn: Boolean,
    adminOn: Boolean,
    stats: ProgressStats,
    blocksWeek: Int,
    onOpenProgress: () -> Unit,
    onOpenSettings: () -> Unit,
    onEnableProtection: () -> Unit,
    onEnableAdmin: () -> Unit,
    onAddTerm: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<ParsedTerm?>(null) }
    var showDisclosure by remember { mutableStateOf(false) }

    fun submit() {
        val parsed = TermParser.parse(input)
        when {
            parsed == null ->
                error = "Enter a word (2+ letters) or a full website like example.com"
            keywords.any { it.term == parsed.value } ->
                error = "That one is already on your list"
            else -> pending = parsed
        }
    }

    pending?.let { term ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Add it for good?") },
            text = {
                Text(
                    "\"${term.value}\" will be blocked permanently. Once added, a " +
                        (if (term.isSite) "site" else "word") + " can't be removed."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onAddTerm(term.value)
                    input = ""
                    error = null
                    pending = null
                }) { Text("Add permanently") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Cancel") }
            }
        )
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text("Before you turn this on") },
            text = {
                Text(
                    "Soapy uses Android's Accessibility Service to read the text in browser " +
                        "address and search bars on this phone, and to notice uninstall screens. " +
                        "It only compares that text with the words you chose to block. " +
                        "Nothing is sent anywhere. The only thing Soapy saves about a block is the " +
                        "time it happened, to count your urges stopped. If you do not agree, " +
                        "do not turn it on."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDisclosure = false
                    onEnableProtection()
                }) { Text("I agree") }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosure = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.soapy_art),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Soapy", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = SoapyColors.Lavender
                    )
                }
            }
        }

        item { StreakCard(stats = stats, blocksWeek = blocksWeek, onClick = onOpenProgress) }

        item {
            SoapyCard(container = SoapyColors.Card) {
                StatusRow(
                    title = "Protection",
                    subtitle = if (protectionOn) {
                        "Watching browser search and address bars"
                    } else {
                        "Turn on the accessibility service so blocked words are caught"
                    },
                    on = protectionOn,
                    actionLabel = "Turn on",
                    onAction = { showDisclosure = true }
                )
                HorizontalDivider(color = SoapyColors.Outline)
                StatusRow(
                    title = "Uninstall protection",
                    subtitle = if (adminOn) {
                        "Soapy is harder to remove"
                    } else {
                        "Make Soapy harder to uninstall"
                    },
                    on = adminOn,
                    actionLabel = "Turn on",
                    onAction = onEnableAdmin
                )
            }
        }

        item {
            SoapyCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add a word or website", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                            error = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. xxx or example.com") },
                        singleLine = true,
                        isError = error != null,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        supportingText = {
                            Text(error ?: "Once added, it can't be removed.")
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(text = "Add to blocklist", onClick = { submit() })
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Your blocklist", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("${keywords.size}", fontSize = 15.sp, color = SoapyColors.Lavender)
            }
        }

        if (keywords.isEmpty()) {
            item {
                SoapyCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Nothing here yet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Add the words and websites you reach for when the urge hits.",
                            fontSize = 13.sp,
                            color = SoapyColors.TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(keywords, key = { it.id }) { keyword ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SoapyColors.Surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TypeTag(isSite = isSiteTerm(keyword.term))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            keyword.term,
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Permanent",
                            tint = SoapyColors.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

/** Stored sites are plain hosts like "example.com"; stored words have no dot-and-letters ending. */
private fun isSiteTerm(term: String): Boolean =
    !term.contains(' ') && Regex("^([a-z0-9-]+\\.)+[a-z]{2,}$").matches(term)
