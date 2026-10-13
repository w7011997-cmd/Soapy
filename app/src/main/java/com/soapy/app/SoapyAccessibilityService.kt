package com.soapy.app

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.soapy.app.data.SoapyDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Watches the address and search bars of browsers. When the text matches a word
 * on the user's blocklist, it sends the user to the home screen and shows a
 * message on top. Everything runs on the device, so it works offline.
 */
class SoapyAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val handler = Handler(Looper.getMainLooper())
    private val autoDismiss = Runnable { hideOverlay() }

    @Volatile
    private var keywords: List<String> = emptyList()
    private var overlay: View? = null
    private var lastTriggerAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        scope.launch {
            SoapyDatabase.get(applicationContext).keywordDao().observeAll().collect { list ->
                keywords = list.map { it.term.lowercase() }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || overlay != null) return
        val list = keywords
        if (list.isEmpty()) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastTriggerAt < TRIGGER_COOLDOWN_MS) return
        if (!containsBlocked(event, list)) return

        lastTriggerAt = now
        performGlobalAction(GLOBAL_ACTION_HOME)
        showOverlay()
    }

    override fun onInterrupt() {
        hideOverlay()
    }

    override fun onDestroy() {
        hideOverlay()
        scope.cancel()
        super.onDestroy()
    }

    // ---- detection ----

    private fun containsBlocked(event: AccessibilityEvent, list: List<String>): Boolean {
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            for (t in event.text) {
                if (matches(t?.toString(), list)) return true
            }
        }
        val root = rootInActiveWindow ?: return false
        return scan(root, list, intArrayOf(0))
    }

    /** Looks at editable fields only (address bar, search box), up to a node limit. */
    private fun scan(node: AccessibilityNodeInfo, list: List<String>, visited: IntArray): Boolean {
        if (visited[0]++ > MAX_NODES) return false
        if (node.isEditable && matches(node.text?.toString(), list)) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (scan(child, list, visited)) return true
        }
        return false
    }

    private fun matches(text: String?, list: List<String>): Boolean {
        if (text.isNullOrBlank()) return false
        val lower = text.lowercase()
        return list.any { lower.contains(it) }
    }

    // ---- message overlay ----

    private fun showOverlay() {
        val message = MessageRotator.next()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val pad = dp(32)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F2101418"))
            setPadding(pad, pad, pad, pad)
        }

        val messageView = TextView(this).apply {
            text = message.text
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
        }

        val dismissButton = Button(this).apply {
            text = getString(R.string.overlay_dismiss)
            setOnClickListener { hideOverlay() }
        }

        content.addView(
            messageView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        content.addView(
            dismissButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(32) }
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        try {
            wm.addView(content, params)
            overlay = content
            handler.postDelayed(autoDismiss, AUTO_DISMISS_MS)
        } catch (e: Exception) {
            overlay = null
        }
    }

    private fun hideOverlay() {
        handler.removeCallbacks(autoDismiss)
        val view = overlay ?: return
        overlay = null
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(view)
        } catch (e: Exception) {
            // already removed
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val TRIGGER_COOLDOWN_MS = 1500L
        private const val AUTO_DISMISS_MS = 20_000L
        private const val MAX_NODES = 400
    }
}
