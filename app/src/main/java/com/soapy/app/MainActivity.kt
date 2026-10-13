package com.soapy.app

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.soapy.app.data.BlockedKeyword
import com.soapy.app.data.SoapyDatabase
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var protectionOn by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = colors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SoapyScreen(
                        protectionOn = protectionOn,
                        onEnable = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        protectionOn = isServiceEnabled()
    }

    private fun isServiceEnabled(): Boolean {
        val setting = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val mine = ComponentName(this, SoapyAccessibilityService::class.java)
        return setting.split(':').any { ComponentName.unflattenFromString(it) == mine }
    }
}

@Composable
fun SoapyScreen(protectionOn: Boolean, onEnable: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { SoapyDatabase.get(context).keywordDao() }
    val keywords by dao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var showDisclosure by remember { mutableStateOf(false) }

    fun addWord() {
        val term = input.trim().lowercase()
        if (term.length >= 2) {
            scope.launch { dao.insert(BlockedKeyword(term = term)) }
            input = ""
        }
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text("Before you turn this on") },
            text = {
                Text(
                    "Soapy uses Android's Accessibility Service to read the text in browser " +
                        "address and search bars on this phone. It only compares that text with " +
                        "the words you chose to block. Nothing is sent anywhere or saved. " +
                        "If you do not agree, do not turn it on."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDisclosure = false
                    onEnable()
                }) { Text("I agree") }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosure = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(24.dp))
            Text("Soapy", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Block the words that trigger you.",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (protectionOn) "Protection is on" else "Protection is off",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (protectionOn) {
                            "Soapy is watching browser search and address bars."
                        } else {
                            "Turn on the Soapy accessibility service so blocked words are caught."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!protectionOn) {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { showDisclosure = true }) { Text("Turn on") }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Add a word to block") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { addWord() }) { Text("Add") }
            }
        }

        item {
            Text(
                "Blocked words (${keywords.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (keywords.isEmpty()) {
            item {
                Text(
                    "No words yet. Add the words and site names you search when the urge hits.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        items(keywords, key = { it.id }) { keyword ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(keyword.term, style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = { scope.launch { dao.delete(keyword) } }) {
                    Text("Remove")
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Your words stay on this phone. Nothing is uploaded.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
