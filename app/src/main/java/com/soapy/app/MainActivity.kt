package com.soapy.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.soapy.app.data.LockStore
import com.soapy.app.ui.SoapyRoot
import com.soapy.app.ui.SoapyTheme

class MainActivity : ComponentActivity() {

    private var protectionOn by mutableStateOf(false)
    private var adminOn by mutableStateOf(false)
    private var lockEnabled by mutableStateOf(false)
    private var unlocked by mutableStateOf(true)
    private var leftAt = 0L

    private val adminComponent by lazy {
        ComponentName(this, SoapyDeviceAdminReceiver::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val store = LockStore.get(this)
        lockEnabled = store.isEnabled()
        unlocked = !lockEnabled

        setContent {
            SoapyTheme {
                SoapyRoot(
                    store = store,
                    protectionOn = protectionOn,
                    adminOn = adminOn,
                    lockEnabled = lockEnabled,
                    unlocked = unlocked,
                    onUnlocked = { unlocked = true },
                    onLockChanged = { enabled ->
                        lockEnabled = enabled
                        unlocked = true
                    },
                    onEnableProtection = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onEnableAdmin = { requestDeviceAdmin() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Ask for the PIN again if the user was away for a while
        if (lockEnabled && leftAt != 0L &&
            SystemClock.elapsedRealtime() - leftAt > RELOCK_AFTER_MS
        ) {
            unlocked = false
        }
    }

    override fun onStop() {
        super.onStop()
        leftAt = SystemClock.elapsedRealtime()
    }

    override fun onResume() {
        super.onResume()
        protectionOn = isServiceEnabled()
        adminOn = devicePolicyManager().isAdminActive(adminComponent)
    }

    private fun devicePolicyManager(): DevicePolicyManager =
        getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getString(R.string.device_admin_description)
            )
        }
        startActivity(intent)
    }

    private fun isServiceEnabled(): Boolean {
        val setting = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val mine = ComponentName(this, SoapyAccessibilityService::class.java)
        return setting.split(':').any { ComponentName.unflattenFromString(it) == mine }
    }

    companion object {
        private const val RELOCK_AFTER_MS = 30_000L
    }
}
