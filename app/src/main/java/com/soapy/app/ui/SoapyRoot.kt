package com.soapy.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.soapy.app.data.BlockedKeyword
import com.soapy.app.data.LockStore
import com.soapy.app.data.SoapyDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Screen { HOME, SETTINGS, LOCK_SETUP, LOCK_CHANGE, LOCK_DISABLE, PRIVACY, TERMS }

/**
 * Splash first, then the lock screen (if a lock is set), then the app.
 */
@Composable
fun SoapyRoot(
    store: LockStore,
    protectionOn: Boolean,
    adminOn: Boolean,
    lockEnabled: Boolean,
    unlocked: Boolean,
    onUnlocked: () -> Unit,
    onLockChanged: (Boolean) -> Unit,
    onEnableProtection: () -> Unit,
    onEnableAdmin: () -> Unit
) {
    var splashDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(2400)
        splashDone = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Crossfade(
            targetState = splashDone,
            animationSpec = tween(450),
            label = "root"
        ) { done ->
            when {
                !done -> SplashContent()
                lockEnabled && !unlocked -> LockGate(store = store, onUnlocked = onUnlocked)
                else -> AppNavigation(
                    store = store,
                    lockEnabled = lockEnabled,
                    protectionOn = protectionOn,
                    adminOn = adminOn,
                    onLockChanged = onLockChanged,
                    onEnableProtection = onEnableProtection,
                    onEnableAdmin = onEnableAdmin
                )
            }
        }
    }
}

@Composable
private fun AppNavigation(
    store: LockStore,
    lockEnabled: Boolean,
    protectionOn: Boolean,
    adminOn: Boolean,
    onLockChanged: (Boolean) -> Unit,
    onEnableProtection: () -> Unit,
    onEnableAdmin: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { SoapyDatabase.get(context).keywordDao() }
    val keywords by dao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(Screen.HOME) }

    BackHandler(enabled = screen != Screen.HOME) {
        screen = if (screen == Screen.SETTINGS) Screen.HOME else Screen.SETTINGS
    }

    when (screen) {
        Screen.HOME -> HomeScreen(
            keywords = keywords,
            protectionOn = protectionOn,
            adminOn = adminOn,
            onOpenSettings = { screen = Screen.SETTINGS },
            onEnableProtection = onEnableProtection,
            onEnableAdmin = onEnableAdmin,
            onAddTerm = { term ->
                scope.launch { dao.insert(BlockedKeyword(term = term)) }
            }
        )

        Screen.SETTINGS -> SettingsScreen(
            lockEnabled = lockEnabled,
            adminOn = adminOn,
            protectionOn = protectionOn,
            onBack = { screen = Screen.HOME },
            onSetupLock = { screen = Screen.LOCK_SETUP },
            onChangePin = { screen = Screen.LOCK_CHANGE },
            onDisableLock = { screen = Screen.LOCK_DISABLE },
            onEnableAdmin = onEnableAdmin,
            onEnableProtection = onEnableProtection,
            onOpenPrivacy = { screen = Screen.PRIVACY },
            onOpenTerms = { screen = Screen.TERMS }
        )

        Screen.PRIVACY -> LegalScreen(
            title = "Privacy Policy",
            intro = LegalContent.PRIVACY_INTRO,
            sections = LegalContent.privacy,
            onBack = { screen = Screen.SETTINGS }
        )

        Screen.TERMS -> LegalScreen(
            title = "Terms of Use",
            intro = LegalContent.TERMS_INTRO,
            sections = LegalContent.terms,
            onBack = { screen = Screen.SETTINGS }
        )

        Screen.LOCK_SETUP -> LockFlowScreen(
            mode = LockMode.SETUP,
            store = store,
            onFinished = {
                onLockChanged(true)
                screen = Screen.SETTINGS
            },
            onCancel = { screen = Screen.SETTINGS }
        )

        Screen.LOCK_CHANGE -> LockFlowScreen(
            mode = LockMode.CHANGE,
            store = store,
            onFinished = { screen = Screen.SETTINGS },
            onCancel = { screen = Screen.SETTINGS }
        )

        Screen.LOCK_DISABLE -> LockFlowScreen(
            mode = LockMode.DISABLE,
            store = store,
            onFinished = {
                onLockChanged(false)
                screen = Screen.SETTINGS
            },
            onCancel = { screen = Screen.SETTINGS }
        )
    }
}
