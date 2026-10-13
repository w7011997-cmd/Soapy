package com.soapy.app

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.soapy.app.data.SoapyDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Does two jobs, entirely on the device (so it works offline):
 *
 * 1. Watches the address and search bars of browsers. When the text matches a word or
 *    site on the blocklist, it sends the user home and shows a rotating message.
 * 2. Watches uninstall screens (system installer, app info). When one is about Soapy,
 *    it asks: are you leaving because you want to return to the habit, or because you quit?
 */
class SoapyAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val handler = Handler(Looper.getMainLooper())
    private val autoDismiss = Runnable { hideOverlay() }

    @Volatile
    private var keywords: List<String> = emptyList()
    private var overlay: View? = null
    private var lastTriggerAt = 0L
    private var lastGuardCheckAt = 0L
    private var guardQuietUntil = 0L

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
        val pkg = event.packageName?.toString() ?: return
        if (pkg in GUARD_PACKAGES) {
            checkUninstallScreen(event)
        } else {
            checkBlockedWords(event)
        }
    }

    override fun onInterrupt() {
        hideOverlay()
    }

    override fun onDestroy() {
        hideOverlay()
        scope.cancel()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- blocked words

    private fun checkBlockedWords(event: AccessibilityEvent) {
        val list = keywords
        if (list.isEmpty()) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastTriggerAt < TRIGGER_COOLDOWN_MS) return
        if (!containsBlocked(event, list)) return

        lastTriggerAt = now
        performGlobalAction(GLOBAL_ACTION_HOME)
        showMessageOverlay()
    }

    private fun containsBlocked(event: AccessibilityEvent, list: List<String>): Boolean {
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            for (t in event.text) {
                if (matches(t?.toString(), list)) return true
            }
        }
        val root = rootInActiveWindow ?: return false
        return scanEditable(root, list, intArrayOf(0))
    }

    /** Looks at editable fields only (address bar, search box), up to a node limit. */
    private fun scanEditable(node: AccessibilityNodeInfo, list: List<String>, visited: IntArray): Boolean {
        if (visited[0]++ > MAX_NODES) return false
        if (node.isEditable && matches(node.text?.toString(), list)) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (scanEditable(child, list, visited)) return true
        }
        return false
    }

    private fun matches(text: String?, list: List<String>): Boolean {
        if (text.isNullOrBlank()) return false
        val lower = text.lowercase()
        return list.any { lower.contains(it) }
    }

    // ---------------------------------------------------------------- uninstall question

    private fun checkUninstallScreen(event: AccessibilityEvent) {
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return

        val now = SystemClock.elapsedRealtime()
        if (now < guardQuietUntil || now - lastGuardCheckAt < GUARD_THROTTLE_MS) return
        lastGuardCheckAt = now

        val root = rootInActiveWindow ?: return
        val texts = ArrayList<String>()
        collectTexts(root, texts, intArrayOf(0))

        val aboutSoapy = texts.any { it.contains("soapy") }
        val uninstallScreen = texts.any { it.contains("uninstall") || it.contains("deactivate") }
        if (aboutSoapy && uninstallScreen) showUninstallQuestion()
    }

    private fun collectTexts(node: AccessibilityNodeInfo, out: MutableList<String>, visited: IntArray) {
        if (visited[0]++ > MAX_NODES) return
        node.text?.toString()?.lowercase()?.let { out.add(it) }
        node.contentDescription?.toString()?.lowercase()?.let { out.add(it) }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectTexts(child, out, visited)
        }
    }

    private fun showUninstallQuestion() {
        showOverlay(GUARD_DISMISS_MS) { root ->
            root.addView(logoView())
            root.addView(label(getString(R.string.uninstall_title), 26f, Color.WHITE, true, 16))
            root.addView(label(getString(R.string.uninstall_question), 18f, 0xFFDCD2FF.toInt(), false, 28))
            root.addView(
                primaryButton(getString(R.string.uninstall_answer_habit)) {
                    hideOverlay()
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    handler.postDelayed({ showMessageOverlay() }, 300)
                }
            )
            root.addView(
                secondaryButton(getString(R.string.uninstall_answer_quit)) {
                    hideOverlay()
                    guardQuietUntil = SystemClock.elapsedRealtime() + GUARD_QUIET_MS
                }
            )
        }
    }

    // ---------------------------------------------------------------- overlays

    private fun showMessageOverlay() {
        if (overlay != null) return
        val message = MessageRotator.next()
        showOverlay(AUTO_DISMISS_MS) { root ->
            root.addView(logoView())
            root.addView(label(message.text, 22f, Color.WHITE, true, 28))
            root.addView(primaryButton(getString(R.string.overlay_dismiss)) { hideOverlay() })
        }
    }

    private fun showOverlay(autoDismissMs: Long, build: (LinearLayout) -> Unit) {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val pad = dp(32)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFA000000.toInt(), 0xFA170A36.toInt())
            )
            setPadding(pad, pad, pad, pad)
        }
        build(content)

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
            handler.removeCallbacks(autoDismiss)
            handler.postDelayed(autoDismiss, autoDismissMs)
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

    // ---------------------------------------------------------------- small view helpers

    private fun logoView(): ImageView {
        val image = ImageView(this)
        image.setImageResource(R.drawable.soapy_art)
        image.scaleType = ImageView.ScaleType.CENTER_CROP
        image.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, dp(22).toFloat())
            }
        }
        image.clipToOutline = true
        image.layoutParams = LinearLayout.LayoutParams(dp(84), dp(84)).apply {
            bottomMargin = dp(28)
        }
        return image
    }

    private fun label(text: String, sizeSp: Float, color: Int, bold: Boolean, bottomDp: Int): TextView {
        val view = TextView(this)
        view.text = text
        view.textSize = sizeSp
        view.setTextColor(color)
        view.gravity = Gravity.CENTER
        if (bold) view.setTypeface(view.typeface, Typeface.BOLD)
        view.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(bottomDp) }
        return view
    }

    private fun primaryButton(text: String, onClick: () -> Unit): TextView =
        button(text, 0xFFA66CFF.toInt(), 0xFF16002F.toInt(), false, onClick)

    private fun secondaryButton(text: String, onClick: () -> Unit): TextView =
        button(text, Color.TRANSPARENT, 0xFFC7B2FF.toInt(), true, onClick)

    private fun button(text: String, fill: Int, textColor: Int, outlined: Boolean, onClick: () -> Unit): TextView {
        val view = TextView(this)
        view.text = text
        view.textSize = 16f
        view.setTextColor(textColor)
        view.setTypeface(view.typeface, Typeface.BOLD)
        view.gravity = Gravity.CENTER
        val shape = GradientDrawable()
        shape.cornerRadius = dp(28).toFloat()
        shape.setColor(fill)
        if (outlined) shape.setStroke(dp(2), 0xFFA66CFF.toInt())
        view.background = shape
        view.setPadding(dp(24), dp(16), dp(24), dp(16))
        view.isClickable = true
        view.setOnClickListener { onClick() }
        view.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(14) }
        return view
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val TRIGGER_COOLDOWN_MS = 1500L
        private const val AUTO_DISMISS_MS = 20_000L
        private const val GUARD_DISMISS_MS = 120_000L
        private const val GUARD_QUIET_MS = 90_000L
        private const val GUARD_THROTTLE_MS = 400L
        private const val MAX_NODES = 400

        /** Screens where an app can be uninstalled or a device admin deactivated. */
        private val GUARD_PACKAGES = setOf(
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.samsung.android.packageinstaller",
            "com.miui.packageinstaller",
            "com.android.settings",
            "com.android.vending",
            "com.miui.securitycenter"
        )
    }
}
